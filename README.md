Virtual Instances Generator (VIG) (v 1.8.1)
---

VIG is a [data scaler](http://www.vldb.org/pvldb/vol4/p1470-tay.pdf) specifically designed for benchmarks of [Ontology-based Data Access (OBDA) systems](https://www.slideshare.net/guohuixiao/ontop-answering-sparql-queries-over-relational-databases) such as [Ontop](https://github.com/ontop/ontop). VIG takes as input a source database instance and a scale factor, and produces a scaled database instance that satisfies the schema constraints and that is “similar” to the source instance according to certain ad-hoc similarity measures. The produced database instance is in form of csv files that can be effectively imported into any relational database management system (RDBMS).

VIG is currently the official data scaler of the [NPD benchmark](https://github.com/ontop/npd-benchmark). 

Download, Configure, and Run
----
Please refer to the documentation (http://ontop.github.io/vig/ or `docs` folder) for more information. 

Publications
----
The following list contains a few publications describing VIG. We suggest to skim through them, so as to save time and understand *exactly* how VIG works and what data it can generate for you. 

- [Longest Technical Description and Evaluation of VIG (Semantic Web 10(2): 413-433 (2019))](http://www.semantic-web-journal.net/content/vig-data-scaling-obda-benchmarks-1)
- [Long Technical Description and Evaluation of VIG (@BLINK '16)](http://ceur-ws.org/Vol-1700/paper-06.pdf)
- [Short Technical Description of VIG](https://arxiv.org/abs/1607.06343)
- [Evaluation of VIG with the BSBM Benchmark (@ISWC Posters '16)](http://ceur-ws.org/Vol-1690/paper82.pdf)

Experimental Evaluations
----

Checkout to the `evaluations/results` branch. Evaluations will be in the "evaluations" folder.

Contacts
----------

* [Davide Lanti](http://www.inf.unibz.it/~dlanti/)
