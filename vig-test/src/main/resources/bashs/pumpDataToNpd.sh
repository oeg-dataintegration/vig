#!/bin/bash

pump () {
    DATA_FILE=$1
    DB_NAME=$2
    USER=$3
    TABLE_NAME_EXT=`basename $DATA_FILE`
    TABLE_NAME=${TABLE_NAME_EXT%.csv}                                                              
    echo [script] Pumping table '"'$TABLE_NAME'"'                              

    /home/tir/git/vig/vig-test/src/main/resources/bashs/loadCsvToTable.sh $DATA_FILE $DB_NAME $TABLE_NAME $USER mysql
}

: ${1?"Usage: $0 DB_NAME"}
DB_NAME=$1

# Copy freshly generated csvs into the dumpable folder
cp src/main/resources/csvs/* /home/tir/dumps/csvs

FILES=/home/tir/dumps/csvs/*                                     

for F in $FILES 
do
    #echo $F
    echo "[script][pumpDataToNpd.sh] $F $DB_NAME $USER"
    pump $F $DB_NAME $USER 
done
