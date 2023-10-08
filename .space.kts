job("Deploy to PROD") {
    parameters {
        text("deploy-archive", value = "deploy.zip")
        secret("machines-ips", "{{ project:prod-machines-ips }}")
        secret("ssh-key", "{{ project:prod-ssh-key }}")
        secret("prod-user", "{{ project:prod-prod-user }}")
    }

    container("kirieshki/web-api-build-agent:latest") {

        fileInput {
            source = FileSource.Text("{{ ssh-key }}")
            localPath = "/mnt/space/.ssh/id_rsa"
        }

        env["DEPLOY_ARCHIVE"] = "{{ deploy-archive }}"
        env["MACHINES_IPS"] = "{{ machines-ips }}"
        env["MACHINE_USER"] = "{{ prod-user }}"
        env["MACHINE_PRIVATE_SSH_KEY"] = "/mnt/space/.ssh/id_rsa"

        shellScript {
            interpreter = "/bin/bash"
            location = "./deploy.sh"
        }
    }
}

job("Publish PROD to Docker Registry") {
    parameters{
        secret("config-encryption-key-prod", "{{ project:config-encryption-key-prod }}")
    }

    container("kirieshki/web-api-build-agent:latest") {
        fileInput {
            source = FileSource.Text("{{ ssh-key }}")
            localPath = "/mnt/space/.ssh/id_rsa"
        }

        env["CONFIG_ENCRYPTION_KEY_PROD"] = "{{ project:config-encryption-key-prod }}"

        shellScript {
            interpreter = "/bin/bash"
            location = "./docker-prod.sh"
        }
    }
}
