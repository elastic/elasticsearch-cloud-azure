Azure Cloud Plugin for Elasticsearch
====================================

The Azure Cloud plugin allows to use Azure API for the unicast discovery mechanism.

## Version 2.7.0 for Elasticsearch: 1.x

If you are looking for another version documentation, please refer to the 
[compatibility matrix](https://github.com/elasticsearch/elasticsearch-cloud-azure/#azure-cloud-plugin-for-elasticsearch).


Azure Virtual Machine Discovery
===============================

Azure VM discovery allows to use the azure APIs to perform automatic discovery (similar to multicast in non hostile
multicast environments). Here is a simple sample configuration:

```
cloud:
    azure:
        management:
             subscription.id: XXX-XXX-XXX-XXX
             cloud.service.name: es-demo-app
             keystore:
                   path: /path/to/azurekeystore.pkcs12
                   password: WHATEVER
                   type: pkcs12

discovery:
    type: azure
```

How to start (short story)
--------------------------

* Create Azure instances
* Install Elasticsearch
* Install Azure plugin
* Modify `elasticsearch.yml` file
* Start Elasticsearch

Azure credential API settings
-----------------------------

The following are a list of settings that can further control the credential API:

* `cloud.azure.management.keystore.path`: /path/to/keystore
* `cloud.azure.management.keystore.type`: `pkcs12`, `jceks` or `jks`. Defaults to `pkcs12`.
* `cloud.azure.management.keystore.password`: your_password for the keystore
* `cloud.azure.management.subscription.id`: your_azure_subscription_id
* `cloud.azure.management.cloud.service.name`: your_azure_cloud_service_name

Note that in previous versions, it was:

```
cloud:
    azure:
        keystore: /path/to/keystore
        password: your_password_for_keystore
        subscription_id: your_azure_subscription_id
        service_name: your_azure_cloud_service_name
```

Advanced settings
-----------------

The following are a list of settings that can further control the discovery:

* `discovery.azure.host.type`: either `public_ip` or `private_ip` (default). Azure discovery will use the one you set to ping
other nodes. This feature was not documented before but was existing under `cloud.azure.host_type`.
* `discovery.azure.endpoint.name`: when using `public_ip` this setting is used to identify the endpoint name used to forward requests
to elasticsearch (aka transport port name). Defaults to `elasticsearch`. In Azure management console, you could define
an endpoint `elasticsearch` forwarding for example requests on public IP on port 8100 to the virtual machine on port 9300.
This feature was not documented before but was existing under `cloud.azure.port_name`.
* `discovery.azure.deployment.name`: deployment name if any. Defaults to the value set with `cloud.azure.management.cloud.service.name`.
* `discovery.azure.deployment.slot`: either `staging` or `production` (default).

For example:

```
discovery:
    type: azure
    azure:
        host:
            type: private_ip
        endpoint:
            name: elasticsearch
        deployment:
            name: your_azure_cloud_service_name
            slot: production
```

How to start (long story)
--------------------------

We will expose here one strategy which is to hide our Elasticsearch cluster from outside.

With this strategy, only VM behind this same virtual port can talk to each other.
That means that with this mode, you can use elasticsearch unicast discovery to build a cluster.

Best, you can use the `elasticsearch-cloud-azure` plugin to let it fetch information about your nodes using
azure API.

### Prerequisites

Before starting, you need to have:

* A [Windows Azure account](http://www.windowsazure.com/)
* SSH keys and certificate
* OpenSSL that isn't from MacPorts, specifically `OpenSSL 1.0.1f 6 Jan
  2014` doesn't seem to create a valid keypair for ssh.  FWIW,
  `OpenSSL 1.0.1c 10 May 2012` on Ubuntu 12.04 LTS is known to work.

You should follow [this guide](http://azure.microsoft.com/en-us/documentation/articles/linux-use-ssh-key/) to learn
how to create or use existing SSH keys. If you have already did it, you can skip the following.

Here is a description on how to generate SSH keys using `openssl`:

```sh
# You may want to use another dir than /tmp
cd /tmp
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout azure-private.key -out azure-certificate.pem
chmod 600 azure-private.key azure-certificate.pem
openssl x509 -outform der -in azure-certificate.pem -out azure-certificate.cer
```

Generate a keystore which will be used by the plugin to authenticate with a certificate
all Azure API calls.

```sh
# Generate a keystore (azurekeystore.pkcs12)
# Transform private key to PEM format
openssl pkcs8 -topk8 -nocrypt -in azure-private.key -inform PEM -out azure-pk.pem -outform PEM
# Transform certificate to PEM format
openssl x509 -inform der -in azure-certificate.cer -out azure-cert.pem
cat azure-cert.pem azure-pk.pem > azure.pem.txt
# You MUST enter a password!
openssl pkcs12 -export -in azure.pem.txt -out azurekeystore.pkcs12 -name azure -noiter -nomaciter
```

Upload the `azure-certificate.cer` file both in the elasticsearch Cloud Service (under `Manage Certificates`), 
and under `Settings -> Manage Certificates`.

**Important**: when prompted for a password, you need to enter a non empty one.

See this [guide](http://www.windowsazure.com/en-us/manage/linux/how-to-guides/ssh-into-linux/) to have
more details on how to create keys for Azure.

Once done, you need to upload your certificate in Azure:

* Go to the [management console](https://account.windowsazure.com/).
* Sign in using your account.
* Click on `Portal`.
* Go to Settings (bottom of the left list)
* On the bottom bar, click on `Upload` and upload your `azure-certificate.cer` file.

You may want to use [Windows Azure Command-Line Tool](http://www.windowsazure.com/en-us/develop/nodejs/how-to-guides/command-line-tools/):

* Install [NodeJS](https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager), for example using
homebrew on MacOS X:

```sh
brew install node
```

* Install Azure tools:

```sh
sudo npm install azure-cli -g
```

* Download and import your azure settings:

```sh
# This will open a browser and will download a .publishsettings file
azure account download

# Import this file (we have downloaded it to /tmp)
# Note, it will create needed files in ~/.azure. You can remove azure.publishsettings when done.
azure account import /tmp/azure.publishsettings
```

### Creating your first instance

You need to have a storage account available. Check [Azure Blob Storage documentation](http://www.windowsazure.com/en-us/develop/net/how-to-guides/blob-storage/#create-account)
for more information.

You will need to choose the operating system you want to run on. To get a list of official available images, run:

```sh
azure vm image list
```

Let's say we are going to deploy an Ubuntu image on an extra small instance in West Europe:

* Azure cluster name: `azure-elasticsearch-cluster`
* Image: `b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-13_10-amd64-server-20130808-alpha3-en-us-30GB`
* VM Name: `myesnode1`
* VM Size: `extrasmall`
* Location: `West Europe`
* Login: `elasticsearch`
* Password: `password1234!!`

Using command line:

```sh
azure vm create azure-elasticsearch-cluster \
                b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-13_10-amd64-server-20130808-alpha3-en-us-30GB \
                --vm-name myesnode1 \
                --location "West Europe" \
                --vm-size extrasmall \
                --ssh 22 \
                --ssh-cert /tmp/azure-certificate.pem \
                elasticsearch password1234\!\!
```

You should see something like:

```
info:    Executing command vm create
+ Looking up image
+ Looking up cloud service
+ Creating cloud service
+ Retrieving storage accounts
+ Configuring certificate
+ Creating VM
info:    vm create command OK
```

Now, your first instance is started. You need to install Elasticsearch on it.

> **Note on SSH**
>
> You need to give the private key and username each time you log on your instance:
>
>```sh
>ssh -i ~/.ssh/azure-private.key elasticsearch@myescluster.cloudapp.net
>```
>
> But you can also define it once in `~/.ssh/config` file:
> 
>```
>Host *.cloudapp.net
>  User elasticsearch
>  StrictHostKeyChecking no
>  UserKnownHostsFile=/dev/null
>  IdentityFile ~/.ssh/azure-private.key
>```


```sh
# First, copy your keystore on this machine
scp /tmp/azurekeystore.pkcs12 azure-elasticsearch-cluster.cloudapp.net:/home/elasticsearch

# Then, connect to your instance using SSH
ssh azure-elasticsearch-cluster.cloudapp.net
```

Once connected, install Elasticsearch:

```sh
# Install Latest Java version
# Read http://www.webupd8.org/2012/01/install-oracle-java-jdk-7-in-ubuntu-via.html for details
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java7-installer

# If you want to install OpenJDK instead
# sudo apt-get update
# sudo apt-get install openjdk-7-jre-headless

# Download Elasticsearch
curl -s https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.0.0.deb -o elasticsearch-1.0.0.deb

# Prepare Elasticsearch installation
sudo dpkg -i elasticsearch-1.0.0.deb
```

Check that elasticsearch is running:

```sh
curl http://localhost:9200/
```

This command should give you a JSON result:

```javascript
{
  "status" : 200,
  "name" : "Living Colossus",
  "version" : {
    "number" : "1.0.0",
    "build_hash" : "a46900e9c72c0a623d71b54016357d5f94c8ea32",
    "build_timestamp" : "2014-02-12T16:18:34Z",
    "build_snapshot" : false,
    "lucene_version" : "4.6"
  },
  "tagline" : "You Know, for Search"
}
```

### Install elasticsearch cloud azure plugin

```sh
# Stop elasticsearch
sudo service elasticsearch stop

# Install the plugin
sudo /usr/share/elasticsearch/bin/plugin -install elasticsearch/elasticsearch-cloud-azure/2.0.0

# Configure it
sudo vi /etc/elasticsearch/elasticsearch.yml
```

And add the following lines:

```yaml
# If you don't remember your account id, you may get it with `azure account list`
cloud:
    azure:
        management:
             subscription.id: your_azure_subscription_id
             cloud.service.name: your_azure_cloud_service_name
             keystore:
                   path: /home/elasticsearch/azurekeystore.pkcs12
                   password: your_password_for_keystore

discovery:
    type: azure

# Recommended (warning: non durable disk)
# path.data: /mnt/resource/elasticsearch/data
```

Restart elasticsearch:

```sh
sudo service elasticsearch start
```

If anything goes wrong, check your logs in `/var/log/elasticsearch`.


Scaling Out!
------------

You need first to create an image of your previous machine.
Disconnect from your machine and run locally the following commands:

```sh
# Shutdown the instance
azure vm shutdown myesnode1

# Create an image from this instance (it could take some minutes)
azure vm capture myesnode1 esnode-image --delete

# Note that the previous instance has been deleted (mandatory)
# So you need to create it again and BTW create other instances.

azure vm create azure-elasticsearch-cluster \
                esnode-image \
                --vm-name myesnode1 \
                --location "West Europe" \
                --vm-size extrasmall \
                --ssh 22 \
                --ssh-cert /tmp/azure-certificate.pem \
                elasticsearch password1234\!\!
```

> **Note:** It could happen that azure changes the endpoint public IP address.
> DNS propagation could take some minutes before you can connect again using
> name. You can get from azure the IP address if needed, using:
>
> ```sh
> # Look at Network `Endpoints 0 Vip`
> azure vm show myesnode1
> ```

Let's start more instances!

```sh
for x in $(seq  2 10)
	do
		echo "Launching azure instance #$x..."
		azure vm create azure-elasticsearch-cluster \
		                esnode-image \
		                --vm-name myesnode$x \
		                --vm-size extrasmall \
		                --ssh $((21 + $x)) \
		                --ssh-cert /tmp/azure-certificate.pem \
		                --connect \
		                elasticsearch password1234\!\!
	done
```

If you want to remove your running instances:

```
azure vm delete myesnode1
```

Azure Repository
================

To enable Azure repositories, you have first to set your azure storage settings in `elasticsearch.yml` file:

```
cloud:
    azure:
        storage:
            account: your_azure_storage_account
            key: your_azure_storage_key
```

For information, in previous version of the azure plugin, settings were:

```
cloud:
    azure:
        storage_account: your_azure_storage_account
        storage_key: your_azure_storage_key
```

The Azure repository supports following settings:

* `container`: Container name. Defaults to `elasticsearch-snapshots`
* `base_path`: Specifies the path within container to repository data. Defaults to empty (root directory).
* `chunk_size`: Big files can be broken down into chunks during snapshotting if needed. The chunk size can be specified
in bytes or by using size value notation, i.e. `1g`, `10m`, `5k`. Defaults to `64m` (64m max)
* `compress`: When set to `true` metadata files are stored in compressed format. This setting doesn't affect index
files that are already compressed by default. Defaults to `false`.

Some examples, using scripts:

```sh
# The simpliest one
$ curl -XPUT 'http://localhost:9200/_snapshot/my_backup1' -d '{
    "type": "azure"
}'

# With some settings
$ curl -XPUT 'http://localhost:9200/_snapshot/my_backup2' -d '{
    "type": "azure",
    "settings": {
        "container": "backup_container",
        "base_path": "backups",
        "chunk_size": "32m",
        "compress": true
    }
}'
```

Example using Java:

```java
client.admin().cluster().preparePutRepository("my_backup3")
    .setType("azure").setSettings(ImmutableSettings.settingsBuilder()
        .put(Storage.CONTAINER, "backup_container")
        .put(Storage.CHUNK_SIZE, new ByteSizeValue(32, ByteSizeUnit.MB))
    ).get();
```

Repository validation rules
---------------------------

According to the [containers naming guide](http://msdn.microsoft.com/en-us/library/dd135715.aspx), a container name must 
be a valid DNS name, conforming to the following naming rules:

* Container names must start with a letter or number, and can contain only letters, numbers, and the dash (-) character.
* Every dash (-) character must be immediately preceded and followed by a letter or number; consecutive dashes are not 
permitted in container names.
* All letters in a container name must be lowercase.
* Container names must be from 3 through 63 characters long.


Testing
=======

Integrations tests in this plugin require working Azure configuration and therefore disabled by default.
To enable tests prepare a config file `elasticsearch.yml` with the following content:

```
cloud:
  azure:
    storage:
      account: "YOUR-AZURE-STORAGE-NAME"
      key: "YOUR-AZURE-STORAGE-KEY"
```

Replaces `account`, `key` with your settings. Please, note that the test will delete all snapshot/restore related files in the specified bucket.

To run test:

```sh
mvn -Dtests.azure=true -Dtests.config=/path/to/config/file/elasticsearch.yml clean test
```

Working around a bug in Windows SMB and Java on windows
=======================================================
When using a shared file system based on the SMB protocol (like Azure File Service) to store indices, the way Lucene open index segment files is with a write only flag. This is the *correct* way to open the files, as they will only be used for writes and allows different FS implementations to optimize for it. Sadly, in windows with SMB, this disables the cache manager, causing writes to be slow. This has been described in [LUCENE-6176](https://issues.apache.org/jira/browse/LUCENE-6176), but it affects each and every Java program out there!. This need and must be fixed outside of ES and/or Lucene, either in windows or OpenJDK. For now, we are providing an experimental support to open the files with read flag, but this should be considered experimental and the correct way to fix it is in OpenJDK or Windows.

The Azure Cloud plugin provides two storage types optimized for SMB:

- `smb_mmap_fs`: a SMB specific implementation of the default [mmap fs](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/index-modules-store.html#mmapfs)
- `smb_simple_fs`: a SMB specific implementation of the default [simple fs](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/index-modules-store.html#simplefs)

To use one of these specific storage types, you need to install the Azure Cloud plugin and restart the node.
Then configure Elasticsearch to set the storage type you want.

This can be configured for all indices by adding this to the `elasticsearch.yml` file:

```yaml
index.store.type: smb_simple_fs
```

Note that setting will be applied for newly created indices.

It can also be set on a per-index basis at index creation time:

```sh
curl -XPUT localhost:9200/my_index -d '{
   "settings": {
       "index.store.type": "smb_mmap_fs"
   }
}'
```


License
-------

This software is licensed under the Apache 2 license, quoted below.

    Copyright 2009-2014 Elasticsearch <http://www.elasticsearch.org>

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
