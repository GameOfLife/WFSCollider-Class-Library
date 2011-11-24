

// load the "touchosc wfs" scene (in this folder) to your touchosc device, and play

NetAddr.langPort.postln; // make sure your touchosc device talks to this port

(
UScore(
	UChain([ 'whiteNoise', [ 'amp', 0.19299677668632 ] ], [ 'cutFilter', [ 'freq', [ 23.412675632342, 805.92226404008 ] ] ], [ 'tremolo', [ 'speed', 5.3597421208728 ] ], [ 'wfsDynamicPoint', [ 'point', Point(5, 2.2), 'u_fadeIn', 1.0, 'u_fadeOut', 1.0 ] ])
).gui;
)

x = UOSCsetter( UScore.current );

y = NetAddr( "127.0.0.1", NetAddr.langPort );

// manual controls (more than the touchosc patch can do

y.sendMsg( '/untitled/0/3/point/x', 10);
y.sendMsg( '/untitled/0/3/point/y', -10);

y.sendMsg( '/untitled/prepareAndStart');
y.sendMsg( '/untitled/stop');

y.sendMsg( '/untitled/0/0', \amp , 0.125 );
y.sendMsg( '/untitled/0/0', \amp , 0.5 );

y.sendMsg( '/untitled/0', \stop );
y.sendMsg( '/untitled/0', \play );
y.sendMsg( '/untitled/0', \release );


x.remove;