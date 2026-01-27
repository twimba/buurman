#!/bin/bash
awslocal s3 mb s3://buurman-documents
awslocal s3api put-bucket-acl --bucket buurman-documents --acl public-read
echo "S3 bucket 'buurman-documents' created successfully"
