Overview
--------------------------

ReportMill is a Java application reporting tool available for dynamically generating reports and web pages
from Java applications in formats such as HTML, PDF, CSV, Excel, RTF and Swing. ReportMill combines an
easy-to-use page layout application and a powerful Java API in a single compact jar file, which is easy
to integrate into custom Java applications for the desktop and web.

Adding ReportMill to a Project
--------------------------

ReportMill download instructions and jar are available at: https://reportmill.com/rm15 .

ReportMill releases are published at a maven package repository at ReportMill.com. For use with maven or gradle
simply include a reference to the repository and jar release: 

```
repositories {

    // Maven package repository at reportmill.com
    maven { url 'https://reportmill.com/maven' }
}

dependencies {

    // Check for latest release at https://github.com/reportmill/RM15/releases/latest
    implementation 'com.reportmill:ReportMill15:2023.02.06'
}

```
Build Instructions
--------------------------

This project is a gradle project, which should import easily into any standard Java IDE. It can also be built and run from the command line like this:

    ./gradlew build
    ./gradlew run

Product information is available here: https://reportmill.com/product/

Download of build jars is available here: https://reportmill.com/rm15/

Java / Jar Versions
---------------

ReportMill 15 supports Java version 8 and up.

ReportMill uses poi, version 3.7:

	- poi-3.7.jar: Poi 3.7
