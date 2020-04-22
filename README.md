This is network packet simulator for emulating various network types in order to test the behavior of the UT2, TCP, QUIC protocols.

### Functionality
This network packet simulator support such features as:
* RTT
* Bandwidth limiting
* Separate limits for upload/download channels 
* Setting amount of file sizes
* Setting amount of requests
* Ability to test different combinations of loss 
* Congestion control window
* Multi-client routing support
* Dumping and collecting sent statistic 

### How to start 
#### Requirements
##### Strong
* Linux 
* JDK 8 or higher (note to set $JAVA_HOME to PATH)
##### Optionally
* `valgrid` - for checking memory leaks
* `tshark` - for dumping packets (NB: https://gist.github.com/MinaMikhailcom/0825906230cbbe478faf4d08abe9d11a)
* built QUIC from Chromium sources for QUIC tests (check for it: https://www.chromium.org/quic/playing-with-quic)
    Also, before starting QUIC tests it's needed to generate certificates and add them to CA store.
    1. Run:
        ```
        ${PATH_TO_CHROMIUM}/src/net/tools/quic/generate-certs.sh
        ```
    2. Run:
        ```
        sudo apt install libnss3-tools
        ```
    3. Run
        ```
       sudo bash add-certs-to-stores.sh ${PATH_TO_CHROMIUM}/src/net/tools/quic/certs/out/2048-sha256-root.pem "QUIC Server Root CA"
        ``` 
       
    Next times it can be set up using (remember to configure paths in script:
    ```
    ./set_up_quic.sh
    ```
* built QUICHE from sources for QUICHE tests (check for it: https://github.com/cloudflare/quiche)

### How to run
1. Build project with dependencies:
    ```shell script
    ./gradlew build -x test 
    ```
2. Set up TUN/TAP environment:
    ```
    ./set_up.sh
    ```
3. Run all tests:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand" --info
    ```
    To run concrete tests use this commands:  
    * TCP:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand.PureTcpDataTransferTest" --info
    ```
    * UT2:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand.Ut2UdpDataTransferTest" --info
    ```
    * QUIC:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand.QuicDataTransferTest" --info
    ```
   * QUIC->QUICHE:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand.QuicQuicheDataTransferTest" --info
    ```
   * QUICHE:
    ```
    ./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand.QuicheDataTransferTest" --info
    ```
4. Set down environment:
    ```
    ./set_down.sh
    ```

### Configuration
There are two files for configuring tests:
* `application.properties`  
    This file contains variables for environment. It's needed to change variables related to QUIC before running QUIC tests.
* `configuration.json`  
    This file contains parameters for setting up test cases.

### Additional
#### About TUN/TAP
https://www.kernel.org/doc/Documentation/networking/tuntap.txt

