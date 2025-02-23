# Updating

Refer to the following steps in this document to update a previously installed instance of this application, running
in `Staging` mode or `Production` mode. This guide assumes that the hosting machine is running Linux, and that the
[ :book: ] [`INSTALLING`](./INSTALLING.md) guide has already been completed.

- [ :bookmark: ] [`Staging`](#staging)
    - [ :bookmark: ] [`Latest Release`](#latest-release)
- [ :bookmark: ] [`Production`](#production)
    - [ :bookmark: ] [`Latest Release`](#latest-release-1)

---

### Staging

### Latest Release

### Steps

- [ :whale2: ] Stop the containers running the currently installed services and remove the associated images. The
  data is safely stored in the `volumes/ascent-stg` directory. Use the command:

```
cd ascent
docker compose -f compose-stg.yml down --rmi all
```

- [ :octocat: ] Update the repository. Use the command:

```
git pull
```

- [ :deciduous_tree: ] Check the `stg.env` file with reference to the relative table. Apply customization if
  necessary.

- [ :whale2: ] Recreate the containers for the updated services. Use the command:

```
docker compose -f compose-stg.yml --env-file stg.env up -d
```

---

### Production

### Latest Release

### Steps

- [ :whale2: ] Stop the containers running the currently installed services and remove the associated images. The
  data is safely stored in the `volumes/ascent-prod` directory. Use the command:

```
cd ascent
docker compose -f compose-prod.yml down --rmi all
```

- [ :octocat: ] Update the repository. Use the command:

```
git pull
```

- [ :deciduous_tree: ] Check the `prod.env` file with reference to the relative table. Apply customization if
  necessary.

- [ :whale2: ] Recreate the containers for the updated services. Use the command:

```
docker compose -f compose-prod.yml --env-file prod.env up -d
```