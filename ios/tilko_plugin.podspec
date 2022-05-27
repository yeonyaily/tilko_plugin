#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint tilko_plugin.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'tilko_plugin'
  s.version          = '0.0.1'
  s.summary          = 'Flutter Tilko Plugin'
  s.description      = <<-DESC
Flutter Tilko Plugin
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.vendored_libraries = 'Classes/**/*.a'

  s.dependency 'Flutter'
  s.dependency 'SignalRSwift', '~> 2.0.2'
  s.dependency 'SwiftyRSA'
  s.dependency 'CryptoSwift', '~> 1.3.5'
  s.dependency 'RSAUtil'
  s.dependency 'SwiftDate', '~> 5.0'
  s.dependency 'ASN1Decoder'
  s.dependency 'BlueRSA'
  s.dependency 'SwiftyJSON', '~> 4.0'

  s.platform = :ios, '8.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'arm64' }
  s.swift_version = '5.0'
end
