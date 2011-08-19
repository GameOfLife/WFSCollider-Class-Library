(
WFSPrePanSynth.generateAllDefs;
WFSArrayPanSynth.generateAllDefs( { |defs| d = defs } ); // takes apx 30s, watch post window
)

d; // these are all the array defs

// result: 104 files with a total size of 2.7MB
SynthDef.synthDefDir.openInFinder;

( // loading goes quite fast
{
s.loadDirectory( SynthDef.synthDefDir );
s.sync;
"done".postln;
}.fork;
)

// higher numbers:
WFSArrayPanSynth.maxSize = 96; // default is 64
WFSArrayPanSynth.generateAllDefs( { |defs| d = defs }, 60 ); // takes apx 60s

// typical uses: (check cpu count)

( // 2 arrays double (focused and normal), cubic int (hi-q)
x = [
	Synth( "wfsa_fdc_48" ), 
	Synth( "wfsa_fdc_48" ), 
	Synth( "wfsa_ndc_48" ),
	Synth( "wfsa_ndc_48" ) 
];
)

( // 2 arrays double (focused and normal), linear int
x = [
	Synth( "wfsa_fdl_40" ), 
	Synth( "wfsa_fdl_56" ), 
	Synth( "wfsa_ndl_40" ),
	Synth( "wfsa_ndl_56" ) 
];
)

( // 2 arrays plane, cubic int (hi-q)
x = [ 
	Synth( "wfsa_pdc_48" ),
	Synth( "wfsa_pdc_48" ) 
];
)

( // 2 arrays static (highest possible for one server)
x = [ 
	Synth( "wfsa_usn_48" ),
	Synth( "wfsa_usn_48" )
];
)


/// here's a full example of dynamic panners, including
/// the pausing/unpausing of the array panners
(
c = WFSSpeakerConf.rect(48,48,5,5);
b = s.makeBundle( false, {
	var arrayConfs;
	var group;
	var prePanSynth;
	var normalSynths, focusSynths;
	var normalLevelBuses, focusLevelBuses;
	
	arrayConfs = c.getArrays(0,2); // first section of 2 systems

	normalLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2000 });
	focusLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2001 });
	
	group = Group(s);
	
	// make the panner synths, but don't send yet
	normalSynths = arrayConfs.collect({ |item| Synth.basicNew( "wfsa_ndc_" ++ item.n, s ); });
	focusSynths = arrayConfs.collect({ |item| Synth.basicNew( "wfsa_fdc_" ++ item.n, s ); });
	
	// make the pre-panner synth (has to know the nodeID's of the panner synths)
	prePanSynth = Synth( "wfsp_d_" ++ arrayConfs.size, arrayConfs.collect({ |item,i|
		[ "arrayConf" ++ i, item.asArray, "cornerPoints" ++ i, item.asCornersArray ]
		}).flatten( 1 ) ++ [ 
			\normalLevelBuses, normalLevelBuses, 
			\focusLevelBuses, focusLevelBuses,
			\normalIDs, normalSynths.collect(_.nodeID),
			\focusIDs, focusSynths.collect(_.nodeID),
			//\normalIDs, normalSynths.collect(998), // override: non-existing id
			//\focusIDs, focusSynths.collect(998),
			\point_out, [1000,1001],
			\crossfadeLag, 0.1
			
			
		],
		group, addAction: \addToTail
	);
	
	// send the panner synths
	n = 0;
	normalSynths.do({ |sn, i|
		s.sendMsg( *sn.newMsg( group, [ 
				\arrayConf, arrayConfs[i].asArray, 
				\amp_bus, normalLevelBuses[i],
				\point_bus, [1000,1001],
				\outOffset, n
			], \addToTail ) 
		);
		n = n + arrayConfs[i].n;
	});
	
	n = 0;
	focusSynths.do({ |sn, i|
		s.sendMsg( *sn.newMsg( group, [ 
				\arrayConf, arrayConfs[i].asArray, 
				\amp_bus, focusLevelBuses[i],
				\point_bus, [1000,1001],
				\outOffset, n
			], \addToTail ) 
		);
		n = n + arrayConfs[i].n;
	});
	
	// assign to global vars
	p = prePanSynth; n = normalSynths; f = focusSynths;
	
});
);

// be sure to have the latest wslib!!
thisProcess.monitorOSC(true, [ '/wfs_masterInLevels', '/wfs_masterOutLevels' ] ); 

s.sendBundle(0.2, *b ); // and start

x = { PrivateOut.ar( 0, SinOsc.ar(440) ) }.play( addAction: \addToHead ); // make an input sound;

p.set( \u_gain, 40 );  // make audible on computer speakers (don't do this on wfs!!)

p.set( \point, [0,1] ); 
p.set( \point, [0,5.2] ); 

p.set( \point_in, [1000,1001] ); // listen to bus

p.set( \point_in, [-1,-1] ); // crashes the server!!

( // make a circle outside arrays
a.free;
a = { ReplaceOut.kr( 1000, SinOsc.kr( 0.1, [0, 0.5pi] ).range( -15, 15); ) }.play( addAction: \addToHead );
)

( // make a circle inside arrays
a.free;
a = { ReplaceOut.kr( 1000, SinOsc.kr( 0.1, [0, 0.5pi] ).range( -4.5, 4.5 ); ) }.play( addAction: \addToHead );
)

a.free;

( // mix sum of array 1 and array 2 to left/right speakers
o.free;
o = { ReplaceOut.ar( 0, [ In.ar( 0, 40 ).sum/40, In.ar( 48, 40 ).sum/40 ] ) }.play( addAction: \addToTail );
)

Bus( \control, 1000, 2 ).get; // see the point
Bus( \control, 2000, 4 ).get; // see the prepan fades

RootNode(s).dumpTree(true);

s.meter;