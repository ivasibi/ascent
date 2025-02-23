# Installing

Follow the instructions contained in this document to prepare a machine for the hosting of this application.
The application can be installed in `Staging` mode, to test behaviour between components, and `Production` mode, for
actually serve the application. Finally, to observe the status of the application, it is necessary to connect to the
various services. This guide assumes that the hosting machine is running Linux.

- [ :bookmark: ] [`Staging`](#staging)
    - [ :bookmark: ] [`Latest Release`](#latest-release)
- [ :bookmark: ] [`Production`](#production)
    - [ :bookmark: ] [`Latest Release`](#latest-release-1)
- [ :bookmark: ] [`Connections`](#connections)

---

### Staging

### Latest Release

Before installing it, make sure to have the following tools installed on the hosting machine.

### Tools

|             Tool             | Version |                             Download                             |                         Documentation                         |
|:----------------------------:|:-------:|:----------------------------------------------------------------:|:-------------------------------------------------------------:|
| [ :whale2: ] `Docker Engine` |    -    | [ :link: ] [`Download`](https://docs.docker.com/engine/install/) | [ :book: ] [`Documentation`](https://docs.docker.com/engine/) |
|     [ :octocat: ] `Git`      |    -    |          [ :link: ] [`Download`](https://git-scm.com/)           |    [ :book: ] [`Documentation`](https://git-scm.com/docs)     |

### Steps

Once the hosting machine has all the required tools installed, follow this steps to install the application.

- [ :octocat: ] Clone the repository. Use the command:

```
git clone https://github.com/ivasibi/ascent.git
```

- [ :deciduous_tree: ] Create the `.env` file to customize the application. Populate it using the variables shown in the
  table below. Use the command:

```
cd ascent
touch stg.env
```

|                   Variable                    |        Service        |     Description     |        Optional        |     Default      | Used (Ascent Environment)  |
|:---------------------------------------------:|:---------------------:|:-------------------:|:----------------------:|:----------------:|:--------------------------:|
|  [ :deciduous_tree: ] `AE_ASCENT_HOST_PORT`   | [ :whale2: ] `Ascent` |  Ascent Host Port   | [ :heavy_check_mark: ] |      `8082`      |          [ :x: ]           |
|     [ :deciduous_tree: ] `AE_MYSQL_USER`      | [ :whale2: ] `MySQL`  |     MySQL User      | [ :heavy_check_mark: ] |   `ascent-stg`   |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_MYSQL_PASSWORD`    | [ :whale2: ] `MySQL`  |   MySQL Password    | [ :heavy_check_mark: ] |   `ascent-stg`   |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_MYSQL_DATABASE`    | [ :whale2: ] `MySQL`  |   MySQL Database    | [ :heavy_check_mark: ] |   `ascent-stg`   |   [ :heavy_check_mark: ]   |
| [ :deciduous_tree: ] `AE_MYSQL_ROOT_PASSWORD` | [ :whale2: ] `MySQL`  | MySQL Root Password | [ :heavy_check_mark: ] |   `ascent-stg`   |          [ :x: ]           |
|   [ :deciduous_tree: ] `AE_MYSQL_HOST_PORT`   | [ :whale2: ] `MySQL`  |   MySQL Host Port   | [ :heavy_check_mark: ] |      `3307`      |          [ :x: ]           |
|   [ :deciduous_tree: ] `AE_REDIS_PASSWORD`    | [ :whale2: ] `Redis`  |   Redis Password    | [ :heavy_check_mark: ] |   `ascent-stg`   |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_REDIS_HOST_PORT`   | [ :whale2: ] `Redis`  |   Redis Host Port   | [ :heavy_check_mark: ] |      `6380`      |          [ :x: ]           |

- [ :whale2: ] Create the containers for such services. Once these containers are running, the `volumes/ascent-stg`
  directory will appear, mapping the contents of such containers. Use the command:

```
docker compose -f compose-stg.yml --env-file stg.env up -d
```

---

### Production

### Latest Release

Before installing it, make sure to have the following tools installed on the hosting machine.

### Tools

|             Tool             | Version |                             Download                             |                         Documentation                         |
|:----------------------------:|:-------:|:----------------------------------------------------------------:|:-------------------------------------------------------------:|
| [ :whale2: ] `Docker Engine` |    -    | [ :link: ] [`Download`](https://docs.docker.com/engine/install/) | [ :book: ] [`Documentation`](https://docs.docker.com/engine/) |
|     [ :octocat: ] `Git`      |    -    |          [ :link: ] [`Download`](https://git-scm.com/)           |    [ :book: ] [`Documentation`](https://git-scm.com/docs)     |

### Steps

Once the hosting machine has all the required tools installed, follow this steps to install the application.

- [ :octocat: ] Clone the repository. Use the command:

```
git clone https://github.com/ivasibi/ascent.git
```

- [ :deciduous_tree: ] Create the `.env` file to customize the application. Populate it using the variables shown in the
  table below. Use the command:

```
cd ascent
touch prod.env
```

|                   Variable                    |        Service        |     Description     |        Optional        |    Default    | Used (Ascent Environment)  |
|:---------------------------------------------:|:---------------------:|:-------------------:|:----------------------:|:-------------:|:--------------------------:|
|  [ :deciduous_tree: ] `AE_ASCENT_HOST_PORT`   | [ :whale2: ] `Ascent` |  Ascent Host Port   | [ :heavy_check_mark: ] |    `8083`     |          [ :x: ]           |
|     [ :deciduous_tree: ] `AE_MYSQL_USER`      | [ :whale2: ] `MySQL`  |     MySQL User      | [ :heavy_check_mark: ] | `ascent-prod` |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_MYSQL_PASSWORD`    | [ :whale2: ] `MySQL`  |   MySQL Password    | [ :heavy_check_mark: ] | `ascent-prod` |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_MYSQL_DATABASE`    | [ :whale2: ] `MySQL`  |   MySQL Database    | [ :heavy_check_mark: ] | `ascent-prod` |   [ :heavy_check_mark: ]   |
| [ :deciduous_tree: ] `AE_MYSQL_ROOT_PASSWORD` | [ :whale2: ] `MySQL`  | MySQL Root Password | [ :heavy_check_mark: ] | `ascent-prod` |          [ :x: ]           |
|   [ :deciduous_tree: ] `AE_MYSQL_HOST_PORT`   | [ :whale2: ] `MySQL`  |   MySQL Host Port   | [ :heavy_check_mark: ] |    `3308`     |          [ :x: ]           |
|   [ :deciduous_tree: ] `AE_REDIS_PASSWORD`    | [ :whale2: ] `Redis`  |   Redis Password    | [ :heavy_check_mark: ] | `ascent-prod` |   [ :heavy_check_mark: ]   |
|   [ :deciduous_tree: ] `AE_REDIS_HOST_PORT`   | [ :whale2: ] `Redis`  |   Redis Host Port   | [ :heavy_check_mark: ] |    `6381`     |          [ :x: ]           |

- [ :whale2: ] Create the containers for such services. Once these containers are running, the `volumes/ascent-prod`
  directory will appear, mapping the contents of such containers. Use the command:

```
docker compose -f compose-prod.yml --env-file prod.env up -d
```

---

### Connections

Finally, for observability, connect to the services started above. These tools should be installed on a different machine,
leaving the hosting machine running only the application.

### Tools

|                    Tool                    | Version |                                    Download                                    | Documentation |
|:------------------------------------------:|:-------:|:------------------------------------------------------------------------------:|:-------------:|
|    [ :earth_americas: ] `Brave Browser`    |    -    |              [ :link: ] [`Download`](https://brave.com/download/)              |       -       |
|          [ :chipmunk: ] `DBeaver`          |    -    |             [ :link: ] [`Download`](https://dbeaver.io/download/)              |       -       |
| [ :star: ] `Another Redis Desktop Manager` |    -    | [ :link: ] [`Download`](https://github.com/qishibo/AnotherRedisDesktopManager) |       -       |

### Steps

- [ :earth_americas: ] The application is reachable by navigating to `http://%HOST_IP%:AE_ASCENT_HOST_PORT`.

- [ :chipmunk: ] Create a new connection using the `MySQL` driver, and use the connection details stored in the
  relative `compose-%MODE%.yml` and `%MODE%.env` files, using the table below for reference. Then in the `Driver
  Properties` tab set the `allowPublicKeyRetrieval` property to `true`.

- [ :star: ] Open a new connection, and set the fields using the connection details stored in the relative
  `compose-%MODE%.yml` and `%MODE%.env` files, using the table below for reference.

|        Service         |                    Tool                    |           Field            |                      Value                      |
|:----------------------:|:------------------------------------------:|:--------------------------:|:-----------------------------------------------:|
| [ :whale2: ] `Ascent`  |    [ :earth_americas: ] `Brave Browser`    | [ :pencil2: ] `Ascent URL` |     `http://%HOST_IP%:AE_ASCENT_HOST_PORT`      |
|  [ :whale2: ] `MySQL`  |          [ :chipmunk: ] `DBeaver`          |    [ :pencil2: ] `Host`    |                   `%HOST_IP%`                   |
|  [ :whale2: ] `MySQL`  |          [ :chipmunk: ] `DBeaver`          |    [ :pencil2: ] `Port`    |              `AE_MYSQL_HOST_PORT`               |
|  [ :whale2: ] `MySQL`  |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Database`  |               `AE_MYSQL_DATABASE`               |
|  [ :whale2: ] `MySQL`  |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Username`  |            `AE_MYSQL_USER` or `root`            |
|  [ :whale2: ] `MySQL`  |          [ :chipmunk: ] `DBeaver`          |  [ :pencil2: ] `Password`  | `AE_MYSQL_PASSWORD` or `AE_MYSQL_ROOT_PASSWORD` |
|  [ :whale2: ] `Redis`  | [ :star: ] `Another Redis Desktop Manager` |    [ :pencil2: ] `Host`    |                   `%HOST_IP%`                   |
|  [ :whale2: ] `Redis`  | [ :star: ] `Another Redis Desktop Manager` |    [ :pencil2: ] `Port`    |              `AE_REDIS_HOST_PORT`               |
|  [ :whale2: ] `Redis`  | [ :star: ] `Another Redis Desktop Manager` |  [ :pencil2: ] `Password`  |               `AE_REDIS_PASSWORD`               |