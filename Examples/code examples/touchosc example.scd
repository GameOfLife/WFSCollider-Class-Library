// load the "touchosc wfs" scene (in this folder) to your touchosc device, and play

NetAddr.langPort.postln; // make sure your touchosc device talks to this port

(
UScore(
	UChain( 
		'whiteNoise', 
		[ 'cutFilter', [ 'freq', [ 20, 800 ] ] ], 
		[ 'tremolo', [ 'speed', 5 ] ],
		'wfsDynamicPoint'
	).fadeIn_(1).fadeOut_(1)
).gui;
)

UScore.current.enableOSC; // or check the 'OSC' button in the gui

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

UScore.current.disableOSC;