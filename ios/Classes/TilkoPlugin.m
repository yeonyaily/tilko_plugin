#import "TilkoPlugin.h"
#if __has_include(<tilko_plugin/tilko_plugin-Swift.h>)
#import <tilko_plugin/tilko_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "tilko_plugin-Swift.h"
#endif

@implementation TilkoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftTilkoPlugin registerWithRegistrar:registrar];
}
@end
