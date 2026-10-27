@echo off
chcp 65001 > nul
setlocal

echo ==============================================================
echo    SilverLingo 스마트폰 실기기 원클릭 설치 및 시연 준비 도구
echo ==============================================================
echo.

set SCRIPT_DIR=%~dp0
set SILVERLINGO_APK=%SCRIPT_DIR%SilverLingo_스마트폰_시연용.apk
set DDOCDOC_DIR=%SCRIPT_DIR%ddocdoc_splits

:: ADB 경로 탐색 (PATH -> Local AppData Android SDK)
set ADB=adb
where adb >nul 2>nul
if %errorlevel% neq 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo [오류] adb(Android Debug Bridge)를 찾을 수 없습니다.
        echo Android Studio가 설치되어 있는지 확인하거나,
        echo 스마트폰으로 'SilverLingo_스마트폰_시연용.apk' 파일을 직접 복사하여 설치하세요.
        echo (카카오톡 나와의 채팅 / 구글 드라이브 등으로 폰에 전송 후 설치 가능)
        echo.
        pause
        exit /b 1
    )
)

echo [1/5] 연결된 안드로이드 스마트폰(USB 디버깅) 확인 중...
%ADB% devices -l
echo.

%ADB% -d get-state > nul 2>&1
if %errorlevel% neq 0 (
    echo [안내] 실제 스마트폰이 아직 USB로 연결되지 않았거나 승인되지 않았습니다.
    echo.
    echo 1. 스마트폰과 PC를 USB 케이블로 연결하세요.
    echo 2. 스마트폰 [설정] - [개발자 옵션] - [USB 디버깅]을 켜세요.
    echo    (개발자 옵션이 없다면 [휴대전화 정보] - [소프트웨어 정보] - '빌드번호' 7번 터치)
    echo 3. 스마트폰 화면에 뜨는 '이 컴퓨터에서 항상 허용'을 체크하고 [확인]을 누르세요.
    echo.
    pause
    exit /b 1
)

echo [OK] 스마트폰 실기기 연결 확인 완료!
echo.

echo [2/5] 스마트폰에 최신 SilverLingo 도우미 앱 설치 중...
%ADB% -d install -r -g "%SILVERLINGO_APK%"
if %errorlevel% neq 0 (
    echo [경고] SilverLingo 설치 중 오류가 발생했습니다.
) else (
    echo [성공] SilverLingo 도우미 앱 설치 완료!
)
echo.

echo [3/5] 스마트폰에 최신 똑닥(11.13.0 ARM64 정식) Split APK 설치 중...
if exist "%DDOCDOC_DIR%\com.bbros.sayup.apk" (
    %ADB% -d install-multiple -r "%DDOCDOC_DIR%\com.bbros.sayup.apk" "%DDOCDOC_DIR%\config.arm64_v8a.apk" "%DDOCDOC_DIR%\config.en.apk" "%DDOCDOC_DIR%\config.xxhdpi.apk"
    if %errorlevel% neq 0 (
        echo [참고] 똑닥 설치 건너뜀 (이미 최신 버전이 설치되어 있거나 플레이스토어 버전 사용 가능)
    ) else (
        echo [성공] 똑닥 최신 버전(v11.13.0) 스마트폰 설치 완료!
    )
) else (
    echo [참고] 똑닥 분할 패키지가 없어 건너뜁니다.
)
echo.

echo [4/5] 스마트폰에 필수 권한 설정 화면 자동 띄우기...
echo - 1) 다른 앱 위에 그리기 권한 화면을 스마트폰에 엽니다. (SilverLingo를 찾아 켜주세요)
%ADB% -d shell am start -a android.settings.action.MANAGE_OVERLAY_PERMISSION -d package:com.silverlingo.app > nul 2>&1
timeout /t 2 > nul

echo - 2) 접근성 서비스 설정 화면을 스마트폰에 엽니다. (설치된 앱 - SilverLingo 도우미 켜기)
%ADB% -d shell am start -a android.settings.ACCESSIBILITY_SETTINGS > nul 2>&1
echo.

echo [5/5] 스마트폰에서 SilverLingo 앱 실행 중...
%ADB% -d shell am start -n com.silverlingo.app/.MainActivity > nul 2>&1
echo.
echo ==============================================================
echo    [설치 완료] 모든 준비가 완료되었습니다!
echo    스마트폰 화면에서 권한 2가지를 켜신 후 시연을 진행하세요.
echo    자세한 시연 방법은 '스마트폰_시연_가이드.txt'를 참고하세요.
echo ==============================================================
pause
