
Eclipse Build Instructions
--------------------------

1. Create New Java Project with this project directory (really just need src and lib).

2. Go to Project Properties -> Java Build Path -> Libraries and add all lib jars.

3. (Optional) Go to Project Properties-> Java Compiler -> Errors/Warnings and set:
	- Potential programming problems: Serializable class without serialVersionUID: Ignore
	- Potential programming problems: Incomplete ‘switch’ cases on enum: Ignore
	- Generic types -> Uncheck generic type operation: Ignore
	- Generic types -> Usage of a raw type: Ignore

4. Go to Run Configurations and add new Java Application configuration with main class:
	- ReportMill: com.reportmill.App
	- SnapCode: snap.app.App

5. (ReportMill Only) Optionally delete SnapCode app packages: snap.app, snap.debug, snap.javafx, snap.javaparse,
	snap.javatext, snap.project, snap.studio 

	
Jar Versions
---------------

	- tools.jar: 8u05
	- jgit.jar: org.eclipse.jgit-3.6.1.201501031845-r.jar
	- jsch.jar: jsch-0.1.51.jar