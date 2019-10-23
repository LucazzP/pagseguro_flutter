import 'package:flutter/material.dart';
import 'package:pagseguro_flutter/pagseguro_flutter.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
  var pag = PlugPag();

  void _incrementCounter() async {
    setState(() {
      _counter++;
    });
    pag.requestAuthentication();
  }

  @override
  void initState() {
    var pag = PlugPag(onState: (state) {
      if (Navigator.canPop(context)) {
        Navigator.pop(context);
      }
      showDialog(
          context: context,
          builder: (context) => AlertDialog(
                content: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    if (state.type == Type.loading) CircularProgressIndicator(),
                    Text(state.message)
                  ],
                ),
              ));
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'You have pushed the button this many times:',
            ),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.display1,
            ),
            FlatButton(
              child: Text("Permission"),
              onPressed: () {
                pag.requestPermissions();
              },
            ),
            FlatButton(
              child: Text("AuthPagseguro"),
              onPressed: () {
                pag.requestAuthentication();
              },
            ),
            FlatButton(
              child: Text("CheckAuthPagseguro"),
              onPressed: () async {
                var res = await pag.checkAuthentication();
                print(res);
              },
            ),
            FlatButton(
              child: Text("InvalidateAuthPagseguro"),
              onPressed: () {
                pag.invalidateAuthentication();
              },
            ),
            FlatButton(
              child: Text("StartPinpad"),
              onPressed: () {
                pag.startPinpadDebitPayment(2.00);
                print(pag.device);
              },
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: Icon(Icons.add),
      ),
    );
  }
}
