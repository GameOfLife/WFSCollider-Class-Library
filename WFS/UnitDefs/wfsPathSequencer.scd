/*
\wfsPathSequencer

A player for multiple spatial trajectories in sequence. A crossfade time can be applied to smoothly transition between each of the trajectories. The WFSPathGUI can be called up via the [edit] button in the UChain window, for each of the trajectories.

This signal can be used by the wfsDynamicPoint and wfsDynamicPlane udefs, by checking their 'pointFromBus' argument.

Each WFSPath can be timed by using its 'delay' setting.

wfsPath: a WFSPathBuffer object. This object creates and points to a buffer with the data of the trajectory. This buffer is either filled with data sent directly, or data read from a .wfspath file (generated via the [write data] button in the GUI). Data from file is usually more reliable than sending via network. This is the first path to be played

fadeTime1, fadeTime2 etc.: the duration of the crossfade to the next trajectory.
fadePos1, fadePos2 etc.: the position of the crossfade on the time line. 
	0 means: before the start of the trajectory
	1 means: after the start of the trajectory
	
wfsPath1, wfsPath2 etc.: WFSPathBuffer objects for each node in the sequence. The 'delay' parameter can be used to set the start time of each path. Paths may overlap, but there will always be only one of them audible (except during the fadeTime).

numPaths: the total number of paths. This may range from 2 to 8. The actual value of this argument is 'wfsPathSequencer%', where % is the number. In the GUI this shows up as numbers in a popup menu. The default number is 2.

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/
(
var defs, def;
defs = (2..8).collect({ |n|
	var def;
	def = HiddenUdef( ("wfsPathSequencer" ++ n).asSymbol, {
		var sig, sweep, startPos;
		sig = WFSPathBufferPlayer.kr( \wfsPath );
		startPos = \u_startPos.kr(0);
		sweep = Sweep.kr + startPos;
		//sweep.poll;
		(n-1).do({ |i|
			var fadeTime, fadePos, startFade, endFade, path, delay, fade;
			i = i+1;
			fadeTime = ("fadeTime" ++ i).asSymbol.kr(0.1).max(1.0e-6);
			fadePos = ("fadePos" ++ i).asSymbol.kr(0);
			path = WFSPathBufferPlayer.kr( "wfsPath" ++ i );
			delay = ("wfsPath" ++ i).asSymbol.kr( [ 0, 0, 1, 0, 0 ] )[4] + startPos;
			startFade = delay - (fadeTime * (1-fadePos));
			endFade = startFade + fadeTime;
			fade = SinOsc.kr( 0, sweep.linlin(startFade, endFade, -0.5pi, 0.5pi, \minmax) );
			//fade.poll( label: i.asString );
			sig = LinXFade2.kr( sig, path, fade );
		});
		UMixOut.kr( 0, sig );
	})
		.category_( \wfs_control )
		.setSpec( \wfsPath, WFSPathSpec())
		.setDefault( \wfsPath, WFSPathSpec().default )
		.setControlOutName( [0,1], ['x', 'y'] );
	
	(n-1).do({ |i|
		i = i+1;
		def.setSpec( ("fadeTime" ++ i).asSymbol, SMPTESpec() );
		def.setSpec( ("fadePos" ++ i).asSymbol, [0,1].asSpec );
		def.setSpec( ("wfsPath" ++ i).asSymbol, WFSPathSpec() );
		def.setDefault( ("wfsPath" ++ i).asSymbol, WFSPathSpec().default );
	});
	def;
});

def = MultiUdef( \wfsPathSequencer, defs, \wfs_control, \numPaths, false );

def.argSpecs.last.spec.labels = (2..8).collect(_.asSymbol);

def
)