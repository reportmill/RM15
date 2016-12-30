;This file will be executed next to the application bundle image
;I.e. current directory will contain folder RMStudio15 with application files
[Setup]
AppId={{RMStudio15}}
AppName=RMStudio15
AppVersion=1.0
AppVerName=RMStudio15 1.0
AppPublisher=RMStudio15
AppComments=
AppCopyright=
;AppPublisherURL=http://reportmill.com/
;AppSupportURL=http://reportmill.com/
;AppUpdatesURL=http://reportmill.com/
DefaultDirName={localappdata}\RMStudio15
DisableStartupPrompt=Yes
DisableDirPage=Auto
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=ReportMill
;Optional License
LicenseFile=
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=RMStudio15
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=RMStudio15\RMStudio15.ico
UninstallDisplayIcon={app}\RMStudio15.ico
UninstallDisplayName=RMStudio15
WizardImageStretch=No
WizardSmallImageFile=RMStudio15-setup-icon.bmp   

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "RMStudio15\RMStudio15.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "RMStudio15\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userstartmenu}\RMStudio15"; Filename: "{app}\RMStudio15.exe"; IconFilename: "{app}\RMStudio15.ico"; Check: returnTrue()
Name: "{commondesktop}\RMStudio15"; Filename: "{app}\RMStudio15.exe"; IconFilename: "{app}\RMStudio15.ico"; Check: returnTrue()

[Run]
Filename: "{app}\RMStudio15.exe"; Description: "{cm:LaunchProgram,RMStudio15}"; Flags: nowait postinstall skipifsilent

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
