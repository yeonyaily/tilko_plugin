import Foundation
import SignalRSwift
import SwiftyRSA
import CryptoSwift
import RSAUtil
import SwiftDate
import ASN1Decoder
import Alamofire
import CryptorRSA
import SwiftyJSON

struct CertInfo: Identifiable {
    var id = UUID()
    
    var filePath:String
    var cn:String
    var validDate:String
      
}

extension Data {
    var bytes : [UInt8]{
        return [UInt8](self)
    }
}

extension Array where Element == UInt8 {
    var data : Data{
        return Data(self)
    }
}

let mUrl = "https://cert.tilko.net/"

extension String {
    mutating func removingRegexMatches(pattern: String, replaceWith: String = "") {
        do {
            let regex = try NSRegularExpression(pattern: pattern, options: .caseInsensitive)
            let range = NSRange(location: 0, length: count)
            self = regex.stringByReplacingMatches(in: self, options: [], range: range, withTemplate: replaceWith)
        } catch { return }
    }

    var encoded: String? {
        return self.addingPercentEncoding(withAllowedCharacters: .controlCharacters)
        
    }
    func trim() -> String {
        return self.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
    }
    
    mutating func insert(string:String,ind:Int) {
        self.insert(contentsOf: string, at:self.index(self.startIndex, offsetBy: ind) )
    }
    
    
    var base64Decoded: String? {
        guard let decodedData = Data(base64Encoded: self) else { return nil }
        return String(data: decodedData, encoding: .utf8)
    }
    
    var base64Encoded: String? {
        let plainData = data(using: .utf8)
        return plainData?.base64EncodedString()
    }
}


@available(iOS 13.0 ,*)
class HubManager:ObservableObject {
    let pubUrl = "https://api.tilko.net/api/Auth/GetPublicKey?ApiKey="    // Public Key API
    var postUrl = "" // Drug API
//    let drugUrl = "https://api.tilko.net/api/v1.0/nhis/ggpab003m0105" // Drug API
    //명시된 POST 주소 : api/v1.0/nhis/ggpab003m0105
    //명시된 Host : api.tilko.net

    var apiKey = ""
    // let apiKey = "011fb7588a904433a7238a8a81784515" //  Tilko 홈페이지에 등록되어 있는 값
    var publicKeyFromServer = ""

    @Published var publicKey:String? = nil
    @Published var privateKey:String? = nil

    var keypair:MIHKeyPair? = nil

    @Published var code:String = ""
    @Published var certificates:[CertInfo] = [CertInfo]()

    func list() {
        parseCertificates()
    }

//    func startConnection() {
//        createKeypair()
//        connectToServer()
//    }

    func createKeypair() {

        SwiftRSACrypto.rsa_generate_key({ (keyPair, _) in
            if let keyPair = keyPair {
                if let pubKey = SwiftRSACrypto.getPublicKey(keyPair) {
                    publicKey = pubKey
                }
                print("\n")
                if let priKey = SwiftRSACrypto.getPrivateKey(keyPair) {
                    privateKey = priKey
                }
                self.keypair = keyPair
            }
        }, ofKeySize: .key2048, archiverFileName: nil)


    }

    func connectToServer(completion:((String?) -> Void)?) {

        let hubConnection = HubConnection(withUrl: mUrl);
        let hub = hubConnection.createHubProxy(hubName: "AuthHub")!

        let headers = [
            "client_type"   : "Mobile",
            "public_cert" : publicKey!.trim().encoded!
        ]

        hubConnection.headers = headers
        hubConnection.started = {
            print("Connection Started.")
        }
        hubConnection.closed = {
            print("Connection Closed")
        }
        hubConnection.connectionSlow = { print("Connection slow...") }
        hubConnection.error = { error in
            let anError = error as NSError
            print(anError.localizedDescription)

            if anError.code == NSURLErrorTimedOut {
                hubConnection.start()
            }
        }

        _ = hub.on(eventName: "ShowCode") { (args) in
            if var code = args[0] as? String {
                print("\(code) is received")
                self.code = code
                completion?(self.code)
            }
        }

        _ = hub.on(eventName: "SaveCertificate") { (args) in

            if let encryptedAesKey = args[0] as? String,
               let encryptedPublicKey = args[1] as? String,
               let encryptedPrivateKey = args[2] as? String,
               let subjectDN = args[3] as? String,
               let sessionId = args[4] as? String {

                print("암호화된 AES 키: \(encryptedAesKey)")
                print("SubjectDN: \(subjectDN)")
                print("암호화된 공개키 HEX 정보: \(encryptedPublicKey)")
                print("암호화된 개인키 HEX 정보: \(encryptedPrivateKey)")

                self.handleCertificates(encryptedAesKey: encryptedAesKey, encryptedPublicKey: encryptedPublicKey, encryptedPrivateKey: encryptedPrivateKey, subjectDN: subjectDN)


            }


        }

        hubConnection.start()
    }

    func handleCertificates(encryptedAesKey:String, encryptedPublicKey:String, encryptedPrivateKey:String, subjectDN:String) {
        print("암호화된 AES 키: \(encryptedAesKey)")
        print("SubjectDN: \(subjectDN)")
        print("암호화된 공개키 HEX 정보: \(encryptedPublicKey)")
        print("암호화된 개인키 HEX 정보: \(encryptedPrivateKey)")

        guard let keypair = self.keypair else { return }


        let iv:[UInt8] = [123, 140, 56, 128, 22, 11,
                          170, 121, 33, 113, 73, 28,
                          208, 42, 247, 134]

        let aesBytes = Array<UInt8>(hex: encryptedAesKey)

        let data =  RSAUtil.decryptData(aesBytes.data, privateKey: self.privateKey!)!

        let aes = try! AES(key: data.bytes, blockMode: CBC(iv: iv), padding: .pkcs7)

        let decryptedPublicBytes = try! aes.decrypt(Array<UInt8>.init(hex: encryptedPublicKey))
        let decryptedPrivateBytes = try! aes.decrypt(Array<UInt8>.init(hex: encryptedPrivateKey))
        let e1 = Array<UInt8>.init(hex: String(bytes: decryptedPublicBytes, encoding: .utf8)!)
        let e2 = Array<UInt8>.init(hex: String(bytes: decryptedPrivateBytes, encoding: .utf8)!)


        var issuedBy = ""

        let dnList = subjectDN.split(separator: ",")
        dnList.forEach { (str) in
            print("dn: \(str)")

            let dn = str.split(separator: "=")
            if dn[0] == "O" {
                issuedBy = String(dn[1])
                print("Issued By: \(issuedBy)")
            }
        }

        if (issuedBy == "") {
            return
        }

        let docURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let npkiURL = docURL.appendingPathComponent("NPKI/\(issuedBy)/USER/\(subjectDN)", isDirectory: true)



        let derURL = npkiURL.appendingPathComponent("signCert").appendingPathExtension("der")
        let keyURL = npkiURL.appendingPathComponent("signPri").appendingPathExtension("key")

        let derData = e1.data
        let keyData = e2.data

        try! FileManager.default.createDirectory(atPath: npkiURL.path, withIntermediateDirectories: true, attributes: nil)

        try! derData.write(to: derURL)
        try! keyData.write(to: keyURL)

        print(derURL.absoluteString)

    }

    func parseCertificates() {

        var infos = [CertInfo]()

        let docURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let npkiURL = docURL.appendingPathComponent("NPKI", isDirectory: true)
        do {
            let folderUrls = try FileManager.default.contentsOfDirectory(atPath: npkiURL.path)
            folderUrls.forEach { (folder) in
                //print(folder)

                do {
                    let userFolderURL = npkiURL.appendingPathComponent(folder).appendingPathComponent("USER")
                    let userFolder = try FileManager.default.contentsOfDirectory(atPath: userFolderURL.path)

                    userFolder.forEach { (subjectDN) in
                        //print(subjectDN)

                            do {
                                let filesURL = userFolderURL.appendingPathComponent(subjectDN)
                                let certFiles = try FileManager.default.contentsOfDirectory(atPath: filesURL.path)
                                certFiles.forEach { (file) in

                                    //print(file)
                                    //print(file.lowercased())
                                    if (!file.contains("der")) {
                                        return
                                    }
                                    let derURL = filesURL.appendingPathComponent("signCert").appendingPathExtension("der")

                                    do {
                                        let data = try Data(contentsOf: derURL)

                                        let x509 = try X509Certificate(data: data)

                                        let subject = x509.subjectDistinguishedName ?? ""

                                        //print(subject)
                                        let startDate = x509.notBefore
                                        let endDate = x509.notAfter
                                        var cn = ""
                                        subject.split(separator: ",").forEach { (x) in
                                            if x.contains("CN=") {
                                                let str = String(x)
                                                cn = str.trim().replacingOccurrences(of: "CN=", with: "")

                                                //print("CN is \(cn)")

                                                cn.removingRegexMatches(pattern: "[A-Za-z0-9-]|[!&^%$*#@()/]")

                                                //print(cn)

                                            }
                                        }


                                        let dateStr = "\(startDate!.toFormat("yyyy.MM.dd")) - \(endDate!.toFormat("yyyy.MM.dd"))"
                                        //print(dateStr)

                                        let info = CertInfo(filePath: filesURL.path, cn: cn, validDate: dateStr)
                                        infos.append(info)



                                    } catch {
                                        print(error)
                                    }



                                }
                            } catch {
                                print("인증서 파일 (der, key) 없음")
                            }


                    }

                    if (infos.count>0) {
                        self.certificates = infos
                    }

                } catch {
                    print("사용자 폴더 없음")
                }

            }


        } catch {
            print("인증서 없음")
        }


    }

    func getPublicKey(completion:((String?) -> Void)?) {

        var request = try! URLRequest(url: pubUrl+apiKey, method: .get)
        request.httpMethod = "GET"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        Alamofire.request(request)
            .validate()
            .responseJSON { response in

                switch response.result {
                case .success(let JSON):
                    print("Success with JSON: \(JSON)")

                    let response = JSON as! NSDictionary

                    print(response.description)

                    self.publicKeyFromServer = response.object(forKey: "PublicKey") as! String
                    print(self.publicKeyFromServer)
                    completion?(self.publicKeyFromServer)


                case .failure(let error):
                    print("Request failed with error: \(error)")
                }

        }

    }

    func healthCheckInfo(filePath:String, certPassword: String, completion:((String?) -> Void)?) {

        let certFileURL = URL(fileURLWithPath: filePath).appendingPathComponent("signCert").appendingPathExtension("der")
        let keyFileURL = URL(fileURLWithPath: filePath).appendingPathComponent("signPri").appendingPathExtension("key")

        do {
            let certData = try Data(contentsOf: certFileURL)
            let keyData = try Data(contentsOf: keyFileURL)

            let key = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00] as [UInt8]
            let iv = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00] as [UInt8]

            let carrier = "1"                   // 통신사
            let phoneNumber = ""     // 휴대폰
            let certPass = certPassword        // 인증서암호

            let aes = try! AES(key: key, blockMode: CBC(iv: iv), padding: .pkcs7)

            let certCipherBytes = try aes.encrypt(certData.bytes)
            let keyCipherBytes = try aes.encrypt(keyData.bytes)

            //let ssnCipherBytes = try aes.encrypt(ssn.replacingOccurrences(of: "-", with: "").bytes)
            let certPassCipherBytes = try aes.encrypt(certPass.bytes)
            let phoneNumberCipherBytes = try aes.encrypt(phoneNumber.bytes)

            var aesCipherKey = ""

            let data2 = Data.init(base64Encoded: publicKeyFromServer)

            let keyDict: [String:Any] = [
                kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
                kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                kSecAttrKeySizeInBits as String: NSNumber(value: 2048),
            ]

            let publickeysi = SecKeyCreateWithData(data2! as CFData, keyDict as CFDictionary, nil)

            let blockSize = SecKeyGetBlockSize(publickeysi!)
            var messageEncrypted = [UInt8](repeating: 0, count: blockSize)
            var messageEncryptedSize = blockSize

            let keyStr = String(bytes: key, encoding: .utf8)!
            var status: OSStatus!
            status = SecKeyEncrypt(publickeysi!, .PKCS1, keyStr, keyStr.count, &messageEncrypted, &messageEncryptedSize)
            if status != noErr {
                print("Encryption Error!")
                return
            }


            let pKey = try PublicKey(base64Encoded: publicKeyFromServer)
            let clear = try ClearMessage(data: key.data)
            let encrypted = try clear.encrypted(with: pKey, padding: .PKCS1)
            let data = encrypted.data
            aesCipherKey = data.base64EncodedString()



//            var headers = HTTPHeaders()
//            headers["Content-Type"] = "application/json"
//            headers["API-Key"] = apiKey
//            print(apiKey)
//            print("---->[API-Key]\n")
//            headers["ENC-KEY"] = aesCipherKey
//            print(aesCipherKey)
//            print("---->[ENC-KEY]")

            var params = Parameters()
            params["CertFile"] = certCipherBytes.toBase64()!
            print("\n" + certCipherBytes.toBase64()! + "\n---->[CertFile]");
            params["KeyFile"] = keyCipherBytes.toBase64()!
            print("\n" + keyCipherBytes.toBase64()! + "\n---->[KeyFile]");
//            params["IdentityNumber"] = ssnCipherBytes.toBase64()!
//            print("\n" + ssnCipherBytes.toBase64()! + "\n---->[IdentityNumber]");
            params["CertPassword"] = certPassCipherBytes.toBase64()!
            print("\n" + certPassCipherBytes.toBase64()! + "\n---->[CertPassword]");

//            params["TelecomCompany"] = carrier
//            print("\n" + carrier + "\n---->[TelecomCompany]");
//            params["CellphoneNumber"] = phoneNumberCipherBytes.toBase64()!
//            print("\n" + phoneNumberCipherBytes.toBase64()! + "\n---->[CellphoneNumber]\n");
            let jsonData = try JSONSerialization.data(withJSONObject: params, options: .prettyPrinted)

            var request = try! URLRequest(url: postUrl, method: .post)
            request.httpMethod = "POST"
            request.httpBody = jsonData
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            request.addValue(apiKey, forHTTPHeaderField: "API-Key")
            request.addValue(aesCipherKey, forHTTPHeaderField: "ENC-KEY")

            Alamofire.request(request).responseJSON { (response) in
                if let result = response.result.value {
                    completion?(JSON(result).rawString([.encoding : String.Encoding.utf8]))
                    //completion?(JSON(result))
                    } else {
                        completion?(nil)
                    }
            }

        } catch {
            print(error.localizedDescription)
        }
    }
    
    func medicalTreatment(filePath:String, certPassword: String, completion:((String?) -> Void)?) {

        let certFileURL = URL(fileURLWithPath: filePath).appendingPathComponent("signCert").appendingPathExtension("der")
        let keyFileURL = URL(fileURLWithPath: filePath).appendingPathComponent("signPri").appendingPathExtension("key")

        do {
            let certData = try Data(contentsOf: certFileURL)
            let keyData = try Data(contentsOf: keyFileURL)

            let key = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00] as [UInt8]
            let iv = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00] as [UInt8]

            let carrier = "1"                   // 통신사
            let phoneNumber = ""     // 휴대폰
            let certPass = certPassword        // 인증서암호

            let aes = try! AES(key: key, blockMode: CBC(iv: iv), padding: .pkcs7)

            let certCipherBytes = try aes.encrypt(certData.bytes)
            let keyCipherBytes = try aes.encrypt(keyData.bytes)

            //let ssnCipherBytes = try aes.encrypt(ssn.replacingOccurrences(of: "-", with: "").bytes)
            let certPassCipherBytes = try aes.encrypt(certPass.bytes)
            let phoneNumberCipherBytes = try aes.encrypt(phoneNumber.bytes)

            var aesCipherKey = ""

            let data2 = Data.init(base64Encoded: publicKeyFromServer)

            let keyDict: [String:Any] = [
                kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
                kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                kSecAttrKeySizeInBits as String: NSNumber(value: 2048),
            ]

            let publickeysi = SecKeyCreateWithData(data2! as CFData, keyDict as CFDictionary, nil)

            let blockSize = SecKeyGetBlockSize(publickeysi!)
            var messageEncrypted = [UInt8](repeating: 0, count: blockSize)
            var messageEncryptedSize = blockSize

            let keyStr = String(bytes: key, encoding: .utf8)!
            var status: OSStatus!
            status = SecKeyEncrypt(publickeysi!, .PKCS1, keyStr, keyStr.count, &messageEncrypted, &messageEncryptedSize)
            if status != noErr {
                print("Encryption Error!")
                return
            }


            let pKey = try PublicKey(base64Encoded: publicKeyFromServer)
            let clear = try ClearMessage(data: key.data)
            let encrypted = try clear.encrypted(with: pKey, padding: .PKCS1)
            let data = encrypted.data
            aesCipherKey = data.base64EncodedString()



//            var headers = HTTPHeaders()
//            headers["Content-Type"] = "application/json"
//            headers["API-Key"] = apiKey
//            print(apiKey)
//            print("---->[API-Key]\n")
//            headers["ENC-KEY"] = aesCipherKey
//            print(aesCipherKey)
//            print("---->[ENC-KEY]")

            var params = Parameters()
            params["CertFile"] = certCipherBytes.toBase64()!
            print("\n" + certCipherBytes.toBase64()! + "\n---->[CertFile]");
            params["KeyFile"] = keyCipherBytes.toBase64()!
            print("\n" + keyCipherBytes.toBase64()! + "\n---->[KeyFile]");
//            params["IdentityNumber"] = ssnCipherBytes.toBase64()!
//            print("\n" + ssnCipherBytes.toBase64()! + "\n---->[IdentityNumber]");
            params["CertPassword"] = certPassCipherBytes.toBase64()!
            print("\n" + certPassCipherBytes.toBase64()! + "\n---->[CertPassword]");

//            params["TelecomCompany"] = carrier
//            print("\n" + carrier + "\n---->[TelecomCompany]");
//            params["CellphoneNumber"] = phoneNumberCipherBytes.toBase64()!
//            print("\n" + phoneNumberCipherBytes.toBase64()! + "\n---->[CellphoneNumber]\n");
            let jsonData = try JSONSerialization.data(withJSONObject: params, options: .prettyPrinted)

            var request = try! URLRequest(url: postUrl, method: .post)
            request.httpMethod = "POST"
            request.httpBody = jsonData
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            request.addValue(apiKey, forHTTPHeaderField: "API-Key")
            request.addValue(aesCipherKey, forHTTPHeaderField: "ENC-KEY")

            Alamofire.request(request).responseJSON { (response) in
                if let result = response.result.value {
                    completion?(JSON(result).rawString([.encoding : String.Encoding.utf8]))
                    //completion?(JSON(result))
                    } else {
                        completion?(nil)
                    }
            }

        } catch {
            print(error.localizedDescription)
        }
    }


}
