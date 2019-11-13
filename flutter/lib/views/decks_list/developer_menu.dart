import 'package:delern_flutter/flutter/device_info.dart';
import 'package:delern_flutter/flutter/styles.dart' as app_styles;
import 'package:delern_flutter/flutter/user_messages.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/material.dart';

bool _debugAllowDevMenu = true;
String _projectId = '';
String _deviceInfo = '';

void _getProjectData() {
  FirebaseApp.instance.options.then((options) {
    _projectId = options.projectID;
  });
  DeviceInfo.getDeviceInfo().then((dInfo) {
    _deviceInfo = dInfo.info.entries
        .map((entry) => '${entry.key}: ${entry.value}')
        .join('; \n');
  });
}

List<Widget> buildDeveloperMenu(BuildContext context) {
  if (!_debugAllowDevMenu) {
    return [];
  }
  _getProjectData();

  // This code will run only in debug mode. Since dev menu items are not visible
  // to the end user, we do not need to localize them.
  return [
    const Divider(height: 1),
    ListTile(
      title: Text(
        'Developer menu',
        style: app_styles.navigationDrawerGroupText,
      ),
      subtitle: const Text('Available only in debug mode'),
    ),
    ListTile(
      leading: const Icon(Icons.cancel),
      title: const Text('Simulate a crash'),
      onTap: () {
        UserMessages.showMessage(Scaffold.of(context), 'Crashing...');

        // TODO(dotdoom): https://github.com/flutter/flutter/issues/41540
        FlutterError.onError = Crashlytics.instance.recordFlutterError;

        Crashlytics().crash();
        UserMessages.showMessage(Scaffold.of(context), 'Failed to crash!');
      },
    ),
    ListTile(
      leading: const Icon(Icons.image),
      title: const Text('Remove Debug artifacts'),
      subtitle: const Text('Restart the app to restore'),
      onTap: () {
        // Get the root state to make sure WidgetsApp (a subwidget of
        // MaterialApp) is restarted. Very hacky, but this code will not even
        // exist in production version of the app.
        context.rootAncestorStateOfType(const TypeMatcher<State>())
            // ignore: invalid_use_of_protected_member
            .setState(() {
          _debugAllowDevMenu = false;
          WidgetsApp.debugAllowBannerOverride = false;
        });
      },
    ),
    AboutListTile(
      icon: const Icon(Icons.build),
      aboutBoxChildren: <Widget>[
        Text('Firebase id: $_projectId'),
        Text('$_deviceInfo'),
      ],
      child: const Text('About Debug Version'),
    )
  ];
}
