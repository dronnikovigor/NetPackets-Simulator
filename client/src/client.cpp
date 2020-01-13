#include "configuration.h"
#include <algorithm>
#include <chrono>
#include <endian.h>
#include <fstream>
#include <iostream>
#include <netinet/in.h>
#include <string.h>
#include <ut2-native/pool.h>
#include <ut2-native/server_args.h>
#include <unistd.h>

const char *TAG = " [client] ";
auto start_time = std::chrono::high_resolution_clock::now();

int64_t get_current_time()
{
	auto current_time = std::chrono::high_resolution_clock::now();
	std::chrono::seconds seconds = std::chrono::duration_cast<std::chrono::seconds>(current_time - start_time);
	return seconds.count();
}

std::basic_ostream<char> &log()
{
	return std::cout << get_current_time() << "s" << TAG;
}

std::basic_ostream<char> &log_err()
{
	return std::cerr << get_current_time() << "s" << TAG;
}

int main()
{
	std::ios_base::sync_with_stdio(false);
	Configuration conf = parseConfiguration("configuration.json");

	/* pool init */
	ut2_ServerArgs args;
	ut2_Pool_BindingInfo bind_info;
	ut2_ServerArgs_set_defaults(&args);

	args.tcp.enabled = conf.tcp_mode;
	args.udp.enabled = conf.udp_mode;

	log() << "PID: " << getpid() << std::endl;
	log() << "running mode: [udp=" << args.udp.enabled << "; tcp=" << args.tcp.enabled << "]" << std::endl;

	in_addr_t client_addr;
	inet_pton(AF_INET, conf.client_ip.c_str(), &client_addr);
	if (args.tcp.enabled) {
	    args.tcp.binding.enabled = true;
	    args.tcp.binding.bind_addr.sin_addr.s_addr = client_addr;
		args.tcp.opt.client_mode = true;
	}
	if (args.udp.enabled) {
	    args.udp.bind_addr.sin_addr.s_addr = client_addr;
	}
	ut2_Pool pool = ut2_Pool_new(&args, &bind_info);

	/* cluster init */
	ut2_Cluster cluster = ut2_Cluster_new(pool);
	//passing servers addreses
	for (auto const &server_addr : conf.servers)
	{
		char str[INET_ADDRSTRLEN];
		// now get it back and print it
		inet_ntop(AF_INET, &(server_addr.host), str, INET_ADDRSTRLEN);
		ut2_Cluster_put_host(cluster, &server_addr);
	}

	/* cluster args */
	ut2_StreamArgs stream_args;
	memset(&stream_args, 0, sizeof(stream_args));
	strcpy(stream_args.handler, "handler");
	stream_args.inp_z = stream_args.out_z = ut2_Z_Deflate;

	//creating mon object
	ut2_Mon mon = ut2_Mon_new(pool);

	ut2_CipherCtx cipher_ctx = nullptr;
	std::string command;
	int return_code = 0;
	unsigned int request_counter = 0;
	while (std::cin >> command)
	{
		log() << "received command: " << command << std::endl;
		if (command == "exit")
		{
			break;
		}
		else if (command == "send")
		{
			int amount;
			uint32_t responce_size;
			std::string file_name;
			std::cin >> amount >> responce_size >> file_name;

			log() << "requesting " << responce_size / 1024 << " Kbytes " << amount << " times, file: " << file_name << std::endl;

			for (int i = 0; i < amount; ++i)
			{
				//perform request
				char request_body[1024];
				memset(request_body, 0, 1024);

				unsigned int be_size = htobe32(responce_size);
				unsigned int be_counter = htobe32(request_counter);
				memcpy(request_body, &be_size, sizeof(unsigned int));
				memcpy(request_body + sizeof(unsigned int), &be_counter, sizeof(int32_t));

				ut2_Request request = ut2_Request_new(cluster, &stream_args);
				ut2_Request_set_mon(request, mon, 0);

				ut2_Request_write(request, &request_body, 1024);
				ut2_Request_execute(request);

				ut2_Request_Status req_status;
				ut2_Request_get_status(request, &req_status);
				while (!req_status.closed)
				{
					ut2_Request_get_status(request, &req_status);
				}

				char buf[16 * 1024];
				char file_buf[16 * 1024];

				std::ifstream check_file;
				check_file.open(file_name);

				bool wrong_data = false;
				uint32_t temp, readed = 0;
				while (readed < responce_size)
				{
					temp = std::min(responce_size - readed, (uint32_t)(16 * 1024));
					temp = ut2_Request_read(request, buf, temp);

					check_file.read(file_buf, temp);

					int cmp_res = strncmp(buf, file_buf, temp);
					if (cmp_res != 0)
					{
						wrong_data = true;
						return_code = -1;
						log_err() << request_counter << " request, received data is different " << std::endl;
						break;
					}

					readed += temp;
					if (temp < sizeof(buf))
						break;
				}
				check_file.close();

				if (!wrong_data)
				{
					if (readed != responce_size)
					{
						return_code = -1;
						log_err() << request_counter << " request, received wrong amount of data: expected " << (responce_size / 1024) << " but received " << (readed / 1024) << std::endl;
						log_err() << request_counter << " request, status: " << req_status.err << std::endl;
					}
					else
					{
						log() << request_counter << " request, successfully received " << (readed / 1024) << " Kbytes" << std::endl;
					}
				}

				ut2_Request_free(request);
				request_counter++;
			}
		}
		else if (command == "cipher")
		{
			std::string signing_key;
			std::cin >> signing_key;
			log() << "setting signing key " << signing_key << std::endl;
			log() << cipher_ctx << std::endl;
			if (cipher_ctx)
			{
				ut2_CipherCtx_free(cipher_ctx);
				cipher_ctx = ut2_CipherCtx_new(pool);
			}
			else
			{
				cipher_ctx = ut2_CipherCtx_new(pool);
			}

			ut2_CipherCtx_set_verification_key(cipher_ctx, signing_key.c_str());
			ut2_CipherCtx_assign_cluster(cipher_ctx, cluster);
		}
	}

	if (cipher_ctx)
	{
		ut2_CipherCtx_free(cipher_ctx);
	}
	ut2_Mon_free(mon);

	exit(return_code);
}
