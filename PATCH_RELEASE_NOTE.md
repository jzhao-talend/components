---
version: 7.3.1
module: https://talend.poolparty.biz/coretaxonomy/42
product: https://talend.poolparty.biz/coretaxonomy/183
---

# TPS-4774

| Info             | Value |
| ---------------- | ---------------- |
| Patch Name       | Patch\_20210507_TPS-4774\_v1-7.3.1 |
| Release Date     | 2021-05-07 |
| Target Version   | 20200219\_1130-7.3.1 |
| Product affected | Talend Data Preparation |

## Introduction

This is a self-contained patch.

**NOTE**: For information on how to obtain this patch, reach out to your Support contact at Talend.

## Fixed issues

This patch contains the following fixes:

- TPS-4774 [7.3.1]HDFS dataset headers are not displayed though Set header is enabled(TDI-45852)

## Prerequisites

Consider the following requirements for your system:

    a. Stop Data Prep and tcomp server.
    b. Backup following folder: <dataprep_server_dir>/services/tcomp

## Installation Steps

    a. Copy ZIP files from patch to <dataprep_server_dir>/services
    b. Unzip new ZIP file under <dataprep_server_dir>/services
    c. Copy the unzipped folder "service" and its content to <dataprep_server_dir>/services and overwrite the existing files.

## Post-installation Steps

    a. Restart tcomp server
    b. Restart Data Prep server

## Uninstallation

Backup the Affected files list below. Uninstall the patch by restore the backup files.

## Affected files for this patch

The following files are installed by this patch:

- {Talend\_Dataprep\_path}/services/tcomp/.m2/org/talend/components/simplefileio-runtime/0.27.4/simplefileio-runtime-0.27.4.jar