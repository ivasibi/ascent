# Developing

### Requirements

- [ :whale2: ] `Docker` (Docker Desktop 4.29.0+)
  - [ :link: ] [`Download`](https://www.docker.com/products/docker-desktop/)
  - [ :book: ] [`Guide`](https://docs.docker.com/)
- [ :computer: ] `IntelliJ` (IntelliJ IDEA Community 2024.1+)
  - [ :link: ] [`Download`](https://www.jetbrains.com/idea/download/)
- [ :coffee: ] `Java` (OpenJDK 21.0.1+)
  - [ :link: ] [`Download`](https://www.oracle.com/java/technologies/downloads/)
- [ :hammer: ] `Maven` (Maven 3.9.2+)
  - [ :link: ] [`Download`](https://maven.apache.org/download.cgi)

### Optionals

- [ :beaver: ] `DBeaver` (DBeaver Community 24.0.3+)
  - [ :link: ] [`Download`](https://dbeaver.io/download/)
- [ :star: ] `ARDE` (Another Redis Desktop Manager 1.6.4+)
  - [ :link: ] [`Download`](https://github.com/qishibo/AnotherRedisDesktopManager)

### Steps

- Clone the `repository`

```
git clone https://github.com/ivasibi/ascent.git
git checkout dev
```

- Create development `containers`

```
cd ascent
docker compose -f compose-dev.yml up -d
```

> [!NOTE] 
> Container volumes are going to appear under the `volumes` directory

- Through Maven, select the `dev` profile

- Run the `main` class

```
src/main/java/AscentApplication.java
```

- Optionally, setup `DBeaver` and `ARDE`