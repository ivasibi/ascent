# Developing

Follow this document step by step to prepare a machine for the development of this project, and to connect to its
various services. Before working on it, make sure to have the following tools and plugins installed on the development
machine. This guide assumes that the development machine is running Windows.

### Tools

|                    Tool                    |        Version         |                                    Download                                    |                                     Documentation                                      |
|:------------------------------------------:|:----------------------:|:------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|
|    [ :earth_americas: ] `Brave Browser`    |           -            |              [ :link: ] [`Download`](https://brave.com/download/)              |                                           -                                            |
|       [ :whale2: ] `Docker Desktop`        |           -            |    [ :link: ] [`Download`](https://www.docker.com/products/docker-desktop/)    |             [ :book: ] [`Documentation`](https://docs.docker.com/desktop/)             |
|            [ :octocat: ] `Git`             |           -            |                 [ :link: ] [`Download`](https://git-scm.com/)                  |                 [ :book: ] [`Documentation`](https://git-scm.com/docs)                 |
|            [ :coffee: ] `Java`             | _Java Corretto 21.0.4_ |          [ :link: ] [`Download`](https://aws.amazon.com/en/corretto/)          |                                           -                                            |
|            [ :hammer: ] `Maven`            |     _Maven 3.9.8_      |         [ :link: ] [`Download`](https://maven.apache.org/download.cgi)         |                                           -                                            |
|       [ :computer: ] `IntelliJ IDEA`       |           -            |       [ :link: ] [`Download`](https://www.jetbrains.com/idea/download/)        | [ :book: ] [`Documentation`](https://www.jetbrains.com/help/idea/getting-started.html) |
|          [ :chipmunk: ] `DBeaver`          |           -            |             [ :link: ] [`Download`](https://dbeaver.io/download/)              |                                           -                                            |
| [ :star: ] `Another Redis Desktop Manager` |           -            | [ :link: ] [`Download`](https://github.com/qishibo/AnotherRedisDesktopManager) |                                           -                                            |

### Plugins

|                  Plugin                  |              Tool              | Version |                                          Download                                          |
|:----------------------------------------:|:------------------------------:|:-------:|:------------------------------------------------------------------------------------------:|
|          [ :whale2: ] `Docker`           | [ :computer: ] `IntelliJ IDEA` |    -    |         [ :link: ] [`Download`](https://plugins.jetbrains.com/plugin/7724-docker)          |
| [ :deciduous_tree: ] `Env Files Support` | [ :computer: ] `IntelliJ IDEA` |    -    |   [ :link: ] [`Download`](https://plugins.jetbrains.com/plugin/9525--env-files-support)    |
|        [ :hot_pepper: ] `Lombok`         | [ :computer: ] `IntelliJ IDEA` |    -    |         [ :link: ] [`Download`](https://plugins.jetbrains.com/plugin/6317-lombok)          |
| [ :grinning: ] `GitHub Markdown Emojis`  | [ :computer: ] `IntelliJ IDEA` |    -    | [ :link: ] [`Download`](https://plugins.jetbrains.com/plugin/20705-github-markdown-emojis) |

### Steps

Once the development machine has all the requirements installed, follow this steps to have the project up and running.

- [ :octocat: ] Clone the repository, and checkout on the `dev` branch of it. Use the command:

```
git clone https://github.com/ivasibi/ascent.git
git checkout dev
```

- [ :gear: ] Copy the configuration files stored in the `configs` directory to target destination. Use the command:

```
cd ascent
copy configs\.wslconfig $env:USERPROFILE
```

- [ :whale2: ] Create the development containers. Once these containers are running, the `volumes/ascent-dev` directory will
  appear, mapping the contents of such containers. Use the command:

```
docker compose -f compose-dev.yml up -d
```

- [ :hammer: ] Select the `dev` profile. By doing this, at startup, the application will load
  the `src/main/java/application.yml` and `src/main/java/application-dev.yml` files. The latter contains the connection
  detail that permits the application to connect to the containers created at the previous point.
  Profiles are defined in the `pom.xml` file.

- [ :computer: ] Run the `Ascent` configuration. These run configurations are also stored in the `configs` directory,
  and are discovered during file indexing. In this way these configurations are shared across different workspaces.
  The application is now running, the `logs` directory will appear, containing the application logs.

### Connections

Finally, for a better application development, connect to the services created above.

- [ :earth_americas: ] The application is reachable by navigating to `http://localhost:8080`.

- [ :chipmunk: ] Create a new connection using the `MySQL` driver, and use the connection details stored in the
  `compose-dev.yml` file, or refer to the table below. Then, in the `Driver Properties` tab set the
  `allowPublicKeyRetrieval` property to `true`.

- [ :star: ] Open a new connection, and set the fields using the values stored in the `compose-dev.yml` file, or refer
  to the table below.

|         Service         |                    Tool                    |           Field            |          Value          |
|:-----------------------:|:------------------------------------------:|:--------------------------:|:-----------------------:|
| [ :computer: ] `Ascent` |    [ :earth_americas: ] `Brave Browser`    | [ :pencil2: ] `Ascent URL` | `http://localhost:8080` |
|  [ :whale2: ] `MySQL`   |          [ :chipmunk: ] `DBeaver`          |    [ :pencil2: ] `Host`    |       `localhost`       |
|  [ :whale2: ] `MySQL`   |          [ :chipmunk: ] `DBeaver`          |    [ :pencil2: ] `Port`    |         `3306`          |
|  [ :whale2: ] `MySQL`   |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Database`  |      `ascent-dev`       |
|  [ :whale2: ] `MySQL`   |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Username`  | `ascent-dev` or `root`  |
|  [ :whale2: ] `MySQL`   |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Password`  |      `ascent-dev`       |
|  [ :whale2: ] `Redis`   | [ :star: ] `Another Redis Desktop Manager` |    [ :pencil2: ] `Host`    |       `localhost`       |
|  [ :whale2: ] `Redis`   | [ :star: ] `Another Redis Desktop Manager` |    [ :pencil2: ] `Port`    |         `6379`          |
|  [ :whale2: ] `Redis`   | [ :star: ] `Another Redis Desktop Manager` |  [ :pencil2: ] `Password`  |      `ascent-dev`       |