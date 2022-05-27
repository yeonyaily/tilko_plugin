# tilko_plugin
**Tilko에서 제공하는 "보안 모듈 스크래핑 API 서비스"를 Flutter에서도 사용할 수 있는 Plugin**

- 건강 관련 API사용을 위해, PC에 있는 공동인증서를 모바일로 복사해주는 기능 제공
> Tilko : https://www.tilko.net/  
> 틸코블렛은 "초기 스타트업을 위한 손쉬운 보안 모듈 스크래핑 API 서비스"를 제공하는 슬로건 아래 복잡하고 어려운 보안 모듈 웹 사이트(공인인증서, 전자인증서 및 각종 보안 프로그램 탑재)의 스크래핑 API를 제공하는 회사입니다.

<br>
  
| APIs          | Functions              |  내용                                                |  제공기관    | 
| :-----------  | :--------------------: | :-------------------------------------------------  | --------- |      
| 인증서 등록      | `callCertRegister`     | *국민건강보험공단에 인증서 등록을 할 수 있습니다. <br> 아래의 API들을 사용하기 위해서 최초 1회 공동인증서 등록을 하여야 합니다.* | 건강보험공단 |          
| 건강검진내역     | `callHealthCheckInfo`  | *최근 10년간 건강보험공단에서 실시한 건강검진의 정보를 제공합니다.* | 건강보험공단 |          
| 진료 및 투약 정보 | `callMedicalTreatment` | *진료 및 투약 정보를 확인할 수 있습니다.*                    | 건강보험공단 |           


<br>
  
## Install
#### 1) Dependency 추가
pubspec.yaml의 dependencies에 작성
``` yaml
dependencies:
  tilko_plugin: ^{latest version}
```
dependency 추가 후, `flutter pub get` 실행하기
#### 2) Dart code에 import
``` dart
import 'package:tilko_plugin/tilko_plugin.dart';
```

<br>

## Getting Start
### [Android]
`android/app/build.gradle` 에서 `compileSdkVersion 29`, `minSdkVersion 24`, `targetSdkVersion 29`로 설정
``` gradle
android {
    compileSdkVersion 29 //수정

    sourceSets {
        main.java.srcDirs += 'src/main/kotlin'
    }

    defaultConfig {
        applicationId "com.example.plugin_example"
        minSdkVersion 24 //수정
        targetSdkVersion 29 //수정
        versionCode flutterVersionCode.toInteger()
        versionName flutterVersionName
    }
```

### [iOS]
`ios - Podfile`에서 `# platform :ios, '9.0'`를 `11 이상`으로 설정
```
// ex)
platform :ios, '11.0'
```

<br>

## Plugin 사용법

### 1. 공동인증서 복사
#### step 1 ) 8자리 인증키 받아오기
- 호출 함수
``` dart
String key;
key = await TilkoPlugin.getKey();
```
> 리턴값(key) 예시 : `12345678`

#### step 2 ) 복사되어 존재하는 공동인증서 불러오기
- 호출 함수
``` dart
Map<String, List<String>> certMap = {};
certMap = await TilkoPlugin.getCertificate(_platformVersion)
```
- certMap Key&Value

| Key          | Value                  |       
| :----------: | :--------------------: |     
| `name`       | 공동인증서 소유자 이름 리스트 |          
| `valid`      | 공동인증서 유효기간 리스트   |      
| `filePath`   | 공동인증서 파일 경로 리스트  | 

<br>

### 2. API 호출
#### step 1 ) 사용할 API를 위한 Setting
- 발급받은 API-KEY를 위한 변수 선언
> ex) apiKey = 09594kf324p04tt0nn2weg453c8j2p9l	;  
> apiKey는 tilko에 가입하여 발급받을 수 있습니다.

#### step 2 ) 사용할 API의 body list Map생성
- reference: https://tilko.net/Market


#### step 3 ) API 호출
- 호출 함수
``` dart

// 건강보험공단 인증서 등록 API
jsonString1 = TilkoPlugin.callCertRegister(apiKey, certMap['filePath']![index], identityNumber, certPassword);

// 건강검진내역 API
jsonString2 = TilkoPlugin.callHealthCheckInfo(apiKey, certMap['filePath']![index], certPassword); 

// 진료 및 투약 정보 API
jsonString3 = TilkoPlugin.callMedicalTreatment(apiKey, certMap['filePath']![index], certPassword);  
```

> API 호출 시 파라미터 설명

| 변수                              | 설명    |       
| :------------------------------- | :------------------------------------------ |     
| `apiKey`                         | tilko에서 발급한 개인 APIKEY |          
| `certMap['filePath']![index]`    | 사용할 공동인증서의 파일 경로 |      
| `identityNumber`                 | 공동인증서 소유자의 주민등록번호  | 
| `certPassword`                   | 공동인증서 비밀번호 | 
