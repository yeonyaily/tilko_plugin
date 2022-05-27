import 'package:tilko_plugin/tilko_plugin.dart';
import 'package:flutter/material.dart';
import 'dart:io' show Platform;

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with TickerProviderStateMixin {
  String frontKey = "    ";
  String backKey = "    ";
  Map<String, List<String>> certMap = {};
  TextEditingController passController = TextEditingController();
  TextEditingController identityController = TextEditingController();

  String passText = "";
  String identityText = "";

  String apiKey = ""; //Put in the API-Key that you issued

  GlobalKey<FormState> _key = GlobalKey<FormState>();
  bool isLoading = false;

  Future<void> _getKey() async {
    String key;
    try {
      key = await TilkoPlugin.getKey();
      setState(() {
        frontKey = key.substring(0, 4);
        backKey = key.substring(4, 8);
      });
    } catch (e) {
      print(e);
    }
  }

  Future<void> _getCertificates() async {
    setState(() {
      isLoading = true;
    });

    await TilkoPlugin.getCertificate().then((value) {
      setState(() {
        isLoading = false;
        certMap = value;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        key: _key,
        appBar: AppBar(
          title: const Text('공동인증서(구 공인인증서) 복사'),
        ),
        body: isLoading
            ? Center(
                child: CircularProgressIndicator(),
              )
            : Padding(
                padding: const EdgeInsets.all(16.0),
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Text(
                          'PC에서 공동인증서\n(구 공인인증서)를 복사해\n휴대폰으로 전달해주세요. ',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 18,
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text('1.사전에 준비된 공동인증서 복사를 위해 서버에서 TilkoSignClient를 실행합니다.'
                            '\n2.인증서 가져오기 버튼을 클릭해주세요.'
                            '\n3.공동인증서(구 공인인증서) 로그인 후 아래 인증번호를 입력해주세요. '
                            '\n4.공동인증서 복사 완료 후 아래 인증서 확인 버튼을 눌러주세요. '),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Center(
                          child: ElevatedButton(
                            child: Text('인증번호 불러오기'),
                            onPressed: () {
                              _getKey();
                            },
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(36, 0, 0, 0),
                        child: Text('인증번호'),
                      ),
                      SizedBox(
                        height: 8,
                      ),
                      Center(
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              decoration: BoxDecoration(
                                border: Border.all(
                                  width: 1,
                                  color: Colors.grey,
                                ),
                              ),
                              child: Padding(
                                padding:
                                    const EdgeInsets.fromLTRB(36, 10, 36, 10),
                                child: Text(
                                  frontKey,
                                  style: TextStyle(
                                    fontSize: 20,
                                    color: Colors.blue,
                                  ),
                                ),
                              ),
                            ),
                            SizedBox(
                              width: 8,
                            ),
                            Text(
                              'ㅡ',
                              style: TextStyle(fontSize: 20),
                            ),
                            SizedBox(
                              width: 8,
                            ),
                            Container(
                              decoration: BoxDecoration(
                                border: Border.all(
                                  width: 1,
                                  color: Colors.grey,
                                ),
                              ),
                              child: Padding(
                                padding:
                                    const EdgeInsets.fromLTRB(36, 10, 36, 10),
                                child: Text(
                                  backKey,
                                  style: TextStyle(
                                    fontSize: 20,
                                    color: Colors.blue,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      SizedBox(
                        height: 25,
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Center(
                          child: ElevatedButton(
                            child: Text('인증서 확인'),
                            onPressed: () {
                              _getCertificates();
                            },
                          ),
                        ),
                      ),
                      ListView.builder(
                        scrollDirection: Axis.vertical,
                        shrinkWrap: true,
                        itemCount: certMap.values.isEmpty
                            ? 0
                            : certMap.values.first.length,
                        itemBuilder: (BuildContext context, int index) {
                          return ListTile(
                            title: Text(certMap['name']![index]),
                            subtitle: Text(certMap['valid']![index]),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                SizedBox(
                                  width: 35,
                                  child: IconButton(
                                    onPressed: () async {
                                      await certRegisterDialog(context, index);
                                    },
                                    icon: Icon(
                                      Icons.check,
                                    ),
                                    padding: EdgeInsets.zero,
                                  ),
                                ),
                                SizedBox(
                                  width: 35,
                                  child: IconButton(
                                    onPressed: () async {
                                      await healthCheckInfoDialog(
                                          context, index);
                                    },
                                    icon: Icon(
                                      Icons.add_chart,
                                    ),
                                    padding: EdgeInsets.zero,
                                  ),
                                ),
                                SizedBox(
                                  width: 35,
                                  child: IconButton(
                                    onPressed: () async {
                                      await medicalTreatmentDialog(
                                          context, index);
                                    },
                                    icon: Icon(Icons.medication_outlined),
                                    padding: EdgeInsets.zero,
                                  ),
                                ),
                                (Platform.isAndroid)
                                    ? SizedBox(
                                        width: 35,
                                        child: IconButton(
                                          onPressed: () {
                                            TilkoPlugin.deleteCert(index);
                                          },
                                          icon: Icon(Icons.delete_outline),
                                          padding: EdgeInsets.zero,
                                        ),
                                      )
                                    : SizedBox()
                              ],
                            ),
                          );
                        },
                      )
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Future<void> certRegisterDialog(BuildContext context, int index) async {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('인증서 등록 API 호출'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                onChanged: (value) {
                  setState(() {
                    identityText = value;
                  });
                },
                controller: identityController,
                decoration: InputDecoration(hintText: "주민등록번호를 입력해주세요."),
              ),
              TextField(
                onChanged: (value) {
                  setState(() {
                    passText = value;
                  });
                },
                controller: passController,
                decoration: InputDecoration(hintText: "공동인증서 비밀번호를 입력해주세요."),
              ),
            ],
          ),
          actions: <Widget>[
            FlatButton(
              color: Colors.blue,
              textColor: Colors.white,
              child: Text('확인'),
              onPressed: () async {
                Navigator.pop(context);
                setState(() {
                  isLoading = true;
                });
                await TilkoPlugin.callCertRegister(
                        apiKey, certMap['file']![index], identityText, passText)
                    .then((value) {
                  setState(() {
                    isLoading = false;
                  });
                });
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> healthCheckInfoDialog(BuildContext context, int index) async {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('건강검진내역 API 호출'),
          content: TextField(
            onChanged: (value) {
              setState(() {
                passText = value;
              });
            },
            controller: passController,
            decoration: InputDecoration(hintText: "공동인증서 비밀번호를 입력해주세요."),
          ),
          actions: <Widget>[
            FlatButton(
              color: Colors.blue,
              textColor: Colors.white,
              child: Text('확인'),
              onPressed: () async {
                Navigator.pop(context);
                setState(() {
                  isLoading = true;
                });
                await TilkoPlugin.callHealthCheckInfo(
                        apiKey, certMap['file']![index], passText)
                    .then((value) {
                  setState(() {
                    isLoading = false;
                  });
                });
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> medicalTreatmentDialog(BuildContext context, int index) async {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('진료 및 투약정보 API 호출'),
          content: TextField(
            onChanged: (value) {
              setState(() {
                passText = value;
              });
            },
            controller: passController,
            decoration: InputDecoration(hintText: "공동인증서 비밀번호를 입력해주세요."),
          ),
          actions: <Widget>[
            FlatButton(
              color: Colors.blue,
              textColor: Colors.white,
              child: Text('확인'),
              onPressed: () async {
                Navigator.pop(context);
                setState(() {
                  isLoading = true;
                });
                await TilkoPlugin.callMedicalTreatment(
                        apiKey, certMap['file']![index], passText)
                    .then((value) {
                  setState(() {
                    isLoading = false;
                  });
                });
              },
            ),
          ],
        );
      },
    );
  }
}
