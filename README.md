### Порядок запуска тестов

#### Необходимые условия для сборки

1. Должна быть проставлена переменная `JAVA_HOME` (установлена версия 8 или выше).
2. Установлена утилита `valgrind` (необходима для отслеживания появлений memory leaks).
3. Установлена утилита `tshark` (для создания дампов пакетов).

#### Сборка и запуск Gradle проекта

```shell script
./gradlew build -x test 
./set_up.sh
./gradlew cleanTest test --tests "one.transport.ut2.testing.stand.TestStand" --info
```

### About TUN/TAP
https://www.kernel.org/doc/Documentation/networking/tuntap.txt