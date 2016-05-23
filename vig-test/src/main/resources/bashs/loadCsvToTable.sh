#!/bin/bash

#usage: ./loadCsvToTable.sh csv db_addr username table_name db_type

postgres () {
    CSV=$1
    DB_ADDR=$2
    USERNAME=$3
    TABLE_NAME=$4

    echo $DB_ADDR $USERNAME $TABLE_NAME

    psql --user=${USERNAME} --dbname=$DB_ADDR --password -c "COPY $TABLE_NAME FROM '$CSV' DELIMITER ',' CSV;"
}

db2 () {
    CSV=$1
    DB_ADDR=$2
    USERNAME=$3
    TABLE_NAME=$4

    echo $DB_ADDR $USERNAME $TABLE_NAME
    
    db2 import from $CSV of del insert into '"'${TABLE_NAME}'"'         ####### MODIFY

   # db2 CONNECT RESET
    
}


if [ $# -eq  5 ]
then
    CSV=$1
    DB_ADDR=$2
    TABLE_NAME=$3
    USER=$4
    DB_TYPE=$5
    if [ $DB_TYPE == postgres ]
    then
	postgres $CSV $DB_ADDR $USER $TABLE_NAME
    else 
	if [ $DB_TYPE == db2 ]
	then
	    db2 $CSV $DB_ADDR $USER $TABLE_NAME
	else
	    if [ $DB_TYPE == mysql ]
	    then
		echo 'SET foreign_key_checks = 0;' > sql.tmp
		echo 'SET GLOBAL sql_mode = STRICT_TRANS_TABLES;'
		echo LOAD DATA INFILE "'"${CSV}"'" INTO TABLE $TABLE_NAME FIELDS TERMINATED BY "'"'`'"'" >> sql.tmp
		cat sql.tmp 
		mysql $DB_ADDR --user=$USER --password=gregario < sql.tmp
		rm sql.tmp
	    else
		echo "usage: ./loadCsvToTable.sh csv db_addr table_name username db_type (db_type = postgres, mysql, db2)"
	    fi
	fi
    fi
else 
    		echo "usage: ./loadCsvToTable.sh csv db_addr table_name username db_type (db_type = postgres, mysql, db2)"
fi



