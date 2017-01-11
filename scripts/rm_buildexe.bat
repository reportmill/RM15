
echo "Creating RMStudio15.exe"
pushd Z:\Temp\RM15
"C:\Program Files\Java\jdk1.8.0_111\bin\javapackager" -deploy -native exe ^
-outdir "C:\Users\Jeff\RMApp" -outfile RMStudio15 -name RMStudio15 ^
-appclass com.reportmill.app.AppLoader -v -srcdir "Z:\Temp\RM15\bin" ^
-srcfiles AppLoader.jar;RMStudio15.jar;spell.jar;BuildInfo.txt

echo "Signing RMStudio15.exe"
Z:\Temp\Signtool\signtool sign /f Z:\Temp\Signtool\RMComoCert.pfx /p rmcomodo ^
/t http://timestamp.verisign.com/scripts/timstamp.dll C:\Users\Jeff\RMApp\bundles\RMStudio15.exe

echo "Verify Signing RMStudio15.exe"
Z:\Temp\Signtool\signtool verify /v /pa C:\Users\Jeff\RMApp\bundles\RMStudio15.exe
