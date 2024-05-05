# Developing

### Requirements

|                    Name                    |        Version        |                                    Download                                    |                                     Guide                                      |
|:------------------------------------------:|:---------------------:|:------------------------------------------------------------------------------:|:------------------------------------------------------------------------------:|
|     [ :earth_americas: ] `Web Browser`     |           -           |                                       -                                        |                                       -                                        |
|       [ :whale2: ] `Docker Desktop`        |           -           |    [ :link: ] [`Download`](https://www.docker.com/products/docker-desktop/)    |                 [ :book: ] [`Guide`](https://docs.docker.com/)                 |
|            [ :octocat: ] `Git`             |           -           |                 [ :link: ] [`Download`](https://git-scm.com/)                  |                 [ :book: ] [`Guide`](https://git-scm.com/docs)                 |
|            [ :coffee: ] `Java`             | _Java OpenJDK 21.0.1_ |  [ :link: ] [`Download`](https://www.oracle.com/java/technologies/downloads/)  |                                       -                                        |
|            [ :hammer: ] `Maven`            |     _Maven 3.9.2_     |         [ :link: ] [`Download`](https://maven.apache.org/download.cgi)         |                                       -                                        |
|         [ :computer: ] `IntelliJ`          |           -           |       [ :link: ] [`Download`](https://www.jetbrains.com/idea/download/)        | [ :book: ] [`Guide`](https://www.jetbrains.com/help/idea/getting-started.html) |
|           [ :beaver: ] `DBeaver`           |           -           |             [ :link: ] [`Download`](https://dbeaver.io/download/)              |                                       -                                        |
| [ :star: ] `Another Redis Desktop Manager` |           -           | [ :link: ] [`Download`](https://github.com/qishibo/AnotherRedisDesktopManager) |                                       -                                        |

### Steps

- [ :octocat: ] Clone the `repository`

```
git clone https://github.com/ivasibi/ascent.git
git checkout dev
```

- [ :whale2: ] Create development `containers`

```
cd ascent
docker compose -f compose-dev.yml up -d
```

> [!NOTE]
> [ :whale2: ] Container volumes are stored in the `volumes` directory

- [ :hammer: ] Select the dev `profile`

> [!TIP]
> [ :hammer: ] Guide on profiles can be found here 
> [ :book: ] [`Guide`](https://www.jetbrains.com/help/idea/work-with-maven-profiles.html)

- [ :computer: ] Run the Ascent `configuration`

> [!TIP]
> [ :computer: ] Guide on configurations can be found here 
> [ :book: ] [`Guide`](https://www.jetbrains.com/help/idea/run-debug-configuration.html)

> [!NOTE]
> [ :computer: ] Configurations are stored in the `configs` directory

- [ :earth_americas: ] Connect to the `server`

```
http://localhost:8080
```

### Connections

#### MySQL

- [ :beaver: ] Connect to the MySQL `container`

> [!NOTE]
> [ :beaver: ] MySQL credentials are stored in the `compose-dev.yml` file
 
> [!WARNING] 
> [ :beaver: ] Set the `allowPublicKeyRetrieval` property to `true` as shown in this guide
> [ :book: ] [`Guide`](https://stackoverflow.com/questions/61749304/connection-between-dbeaver-mysql)

#### Redis

- [ :star: ] Connect to the Redis `container`

> [!NOTE]
> [ :star: ] Redis credentials are stored in the `compose-dev.yml` file