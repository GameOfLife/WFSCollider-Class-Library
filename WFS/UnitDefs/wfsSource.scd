/*
\wfsSource

This udef creates a virtual source on the WFS system. It can be a point source or a plane wave source. The position of the source can static or changing over time (dynamic). The unit changes internally depending on the type of the source. If the point is set normally (default) the unit is in 'static' mode, meaning that the point can not be moved during playback. Changes to the point will not be audible until the unit (and chain) is stopped and started again. When applying an UMap on the point (location) of the source, the source becomes 'dynamic', which will add an extra 'quality' parameter, and make the source able to move over time. The reason for the difference between static and dynamic is in optimization; a 'static' source takes considerably less CPU power than a 'dynamic' source. If you want to move the source in realtime by hand, try using (for example) a 'point_lag' UMap on the point argument. This will change the umap into 'dynamic' mode, allowing it to move during playback.

point: This point represents the absolute location of the point source, or the center position of a plane wave (the point on the line nearest to the center of the room). When point becomes a UMap, the unit goes into 'dynamic' mode, except when the UMap outputs single values (for example: 'random_point').
type: (i) \point or \plane
quality: (i) (\normal or \better) setting for the interpolation quality of the delays (only in 'dynamic' mode)
		normal = linear interpolation
		better = cubic interpolation
	Cubic interpolation uses about 50% more cpu.
latencyComp: (i) cancels the delay caused bu the distance of the source. 0 = no compensation, 1 = full compensation. This can reduce the doppler shift for moving sources, and can ensure synchronicity between this source and other sources.
dbRollOff: decrease in level due to a doubling of the distance. In nature this is -6 dB in free field conditions, and might be -3 dB in extremely reverberant rooms.
maxAmpRadius: defines the radius of the circular area in the centre where the level will always be 0 dB (maximum). The dbRollOff starts where this area ends. So the larger the area, the louder the sounds outside of it.

Changes to arguments marked with (i) only become effective the next time the event is played.

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

var staticPoint, dynamicPoint, staticPlane, dynamicPlane, multiDef;
var notPreviewMode, getPreviewMode, renderModeSpec, calcDelayNormal, calcDelayFocused;
var hasElevation;

notPreviewMode = { WFSLib.previewMode === \off };

getPreviewMode = { |unit| if( unit.renderMode == \previewMode ) { WFSLib.previewMode } { unit.renderMode } };

renderModeSpec = ListSpec(
	[ \previewMode ] ++ WFSOptionsObjectGUI.specs.previewMode.list[1..],
	0,
	[ { "previewMode (%)".format( WFSLib.previewMode ).asSymbol } ] ++ WFSOptionsObjectGUI.specs.previewMode.list[1..]
);

calcDelayNormal = { |server|
	((WFSSpeakerConf.default.maxDistance + 0.0504) / WFSBasicPan.speedOfSound) // + (server.options.blockSize/server.sampleRate)
};

calcDelayFocused = { |server|
	((WFSSpeakerConf.default.maxDistance * 2) / WFSBasicPan.speedOfSound) // + (server.options.blockSize/server.sampleRate)
};

hasElevation = {
	[
		\ambix, \ambix_2o, \ambix_3o, \ambix_4o, \ambix_5o, \ambix_6o, \ambix_7o,
		\binaural, \binaural_3o, \binaural_5o, \binaural_7o
	].includes( WFSLib.previewMode );
};

staticPoint = HiddenFreeUdef( \staticPoint, [
	[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
	[ \elevation: 0, AngleSpec( -0.5pi, 0.5pi), { hasElevation.value.not } ],
	[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \nonsynth ],
	[ \latencyComp:  	0,		[0,1].asSpec, false, \init ],
	[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
	[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
	[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	[ \minLatency:  false, BoolSpec(false), false, \init ],
	[ \dynamic:		false,	BoolSpec(false), true, \nonsynth ], // force dynamic (hidden)
	[ \renderMode:	\previewMode, renderModeSpec, notPreviewMode, \nonsynth ],
	[ \busOffset:		0,	HardwareBusSpec(), notPreviewMode, \nonsynth ], // bus offset
] )
.addUEnv
.addUGlobalEQ
.addUIO( UIn, \ar, 0, 1 )
.inputIsEndPoint_( true )
.setSynthFunc_({ |unit ...keyValuePairs|
	keyValuePairs = (keyValuePairs ? []).clump(2) // exclude point for staticPoint
	.select({ |arr| arr[0] !== \point }).flatten(1);
	unit.synths.do({ |sn|
		var args = keyValuePairs;
		if( sn.class == Group ) { // is an array group or preview synth
			args = (args ? []).clump(2)
			.select({ |arr| [ \elevation, \u_i_ar_0_bus ].includes( arr[0]  ) }).flatten(1);
		};
		if( args.size > 0 ) {
			unit.subDef.prSetSynth( sn, *args );
		};
	})
});

staticPoint.env[ \wfsCreateSynthFunc ] = { |unit, target, startPos = 0|
	var server, n = 0;
	var arrayConfs;
	var arrayGroup;
	var prePanSynth;
	var normalLevels, focusLevels;
	var normalSynths = [], focusSynths = [];
	var defNames;
	var crossfader;
	var focusMul;
	var point;
	var addDelay;
	var addDelayNormal;

	server = target.asTarget.server;

	point = unit.get( \point ).value.asPoint;

	arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
	crossfader = WFSCrossfader(
		point, arrayConfs, focusWidth: WFSSpeakerConf.default.focusWidth
	);
	focusMul = WFSSpeakerConf.default.focusDetector.kr( point );

	arrayGroup = Group(target, addAction: \addToTail);

	if( WFSSpeakerConf.default.hasSubs ) {
		defNames = [ "wfsa_nsns_", "wfsa_usns_" ]; // normal and uni static n
	} {
		defNames = [ "wfsa_nsn_", "wfsa_usn_" ]; // normal and uni static n
	};

	addDelay = calcDelayFocused.value( server ) - 0.06;
	if( unit.get( \minLatency ) == true ) {
		addDelayNormal = calcDelayNormal.value( server ) - 0.06;
	} {
		addDelayNormal = addDelay;
	};

	normalLevels = crossfader.arraysShouldRun( false ) * crossfader.cornerfades;
	focusLevels = crossfader.arraysShouldRun( true );

	// make the pre-panner synth (has to know the nodeID's of the panner synths)
	prePanSynth = Synth( "wfsp_n_0",
		unit.getArgsFor( server, startPos ),
		arrayGroup, addAction: \addBefore
	);

	// make the panner synths
	n = 0;
	arrayConfs.do({ |ac, i|
		if( normalLevels[i] > 0 ) {
			normalSynths = normalSynths.add(
				Synth( defNames[0] ++ ac.n, [
					\gain, (WFSSpeakerConf.default.gain + ac.gain).dbamp,
					\arrayConf, ac,
					\outOffset, n + ac.outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
					\amp, normalLevels[i],
					\arrayLimit, WFSSpeakerConf.default.arrayLimit,
					\arraySoftLimit, WFSSpeakerConf.default.arraySoftLimit,
					\addDelay, addDelayNormal
				] ++ if( WFSSpeakerConf.default.hasSubs ) { [
					\subSpacing, ac.subSpacing,
					\subOffset, ac.subOffset,
					\subFreq, WFSSpeakerConf.default.subFreq,
					\subGain, WFSArrayPan.subGain,
				] } { [] } ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
				arrayGroup, addAction: \addToTail
				)
			);
		};
		n = n + arrayConfs[i].n;
	});

	if( focusMul > 0 ) {
		n = 0;
		arrayConfs.do({ |ac, i|
			if( focusLevels[i] > 0 ) {
				focusSynths = focusSynths.add(							Synth( defNames[1] ++ ac.n, [
					\gain, (WFSSpeakerConf.default.gain + ac.gain).dbamp,
					\arrayConf, ac,
					\outOffset, n + ac.outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
					\amp, focusLevels[i],
					\arrayLimit, WFSSpeakerConf.default.arrayLimit,
					\arraySoftLimit, WFSSpeakerConf.default.arraySoftLimit,
					\focusWidth, WFSSpeakerConf.default.focusWidth,
					\addDelay, addDelay,
				] ++ if( WFSSpeakerConf.default.hasSubs ) { [
					\subSpacing, ac.subSpacing,
					\subOffset, ac.subOffset,
					\subFreq, WFSSpeakerConf.default.subFreq,
					\subGain, WFSArrayPan.subGain,
				] } { [] } ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
				arrayGroup, addAction: \addToTail
				)
				);
			};
			n = n + arrayConfs[i].n;
		});
	};

	arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
	unit.synths = unit.synths.add( arrayGroup );

	prePanSynth;
};

staticPoint.env[ \wfsShouldPlayOnFunc ] = { |unit, target|
	var arrays, crossfader;
	arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
	(arrays.size > 0) && {
		crossfader = WFSCrossfader( unit.get( \point ).uPrepareValue.asPoint, arrays );
		[(crossfader.cornerfades * crossfader.arraysShouldRun( false )).maxItem,
			crossfader.arraysShouldRun( true ).maxItem ].maxItem > 0
	};
};
// preview panner; exact same parameters, different panner synthdefs
staticPoint.env[ \previewCreateSynthFunc ] = { |unit, target, startPos = 0|
	Synth( WFSPreviewSynthDefs.getDefName( getPreviewMode.( unit ), \s, \n ),
		unit.getArgsFor(  target.asTarget.server, startPos ) ++
		[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) + unit.busOffset ],
		target, \addToTail
	);
};

dynamicPoint = HiddenFreeUdef( \dynamicPoint, [
	[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
	[ \elevation: 0, AngleSpec( -0.5pi, 0.5pi), { hasElevation.value.not } ],
	[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \nonsynth ],
	[ \quality:		0,		ListSpec([ \good, \better ], 0 ), false, \nonsynth ], // \l or \c
	[ \distanceFilter:	0,		[0,2,\lin,0,1].asSpec ],
	[ \latencyComp:  	0,		[0,1].asSpec, false, \init ],
	[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
	[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	[ \minLatency:  false, BoolSpec(false), false, \init ],
	[ \dynamic:		false,	BoolSpec(false), true, \nonsynth ], // force dynamic (hidden)
	[ \renderMode:			\previewMode, renderModeSpec, notPreviewMode, \nonsynth ],
	[ \busOffset:		0,	HardwareBusSpec(), notPreviewMode, \nonsynth ], // bus offset
], true )
.addUGlobalEQ
.addUIO( UIn, \ar, 0, 1 )
.inputIsEndPoint_( true )
.setSynthFunc_({ |unit ...keyValuePairs|
	unit.synths.do({ |sn|
		var args = keyValuePairs;
		if( sn.class == Group ) { // is an array group or preview synth
			args = (args ? []).clump(2)
			.select({ |arr| [ \point, \elevation, \u_i_ar_0_bus ].includes( arr[0] ) }).flatten(1);
		};
		if( args.size > 0 ) {
			unit.subDef.prSetSynth( sn, *args );
		};
	})
});

// dynamic point wfs panner
dynamicPoint.env[ \wfsCreateSynthFunc ] = { |unit, target, startPos = 0|
	var server, n = 0;
	var arrayConfs;
	var arrayGroup;
	var prePanSynth;
	var normalSynths, focusSynths;
	var normalLevelBuses, focusLevelBuses;
	var defNames;
	var addDelay;
	var addDelayNormal;

	server = target.asTarget.server;

	arrayConfs = WFSSpeakerConf.default.getArraysFor( server );

	normalLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2000 });
	focusLevelBuses = arrayConfs.collect({ |item, i| (i*2) + 2001 });

	arrayGroup = Group(target, addAction: \addToTail);

	defNames = [
		[ "wfsa_nd", "wfsa_fd" ],
		if( WFSSpeakerConf.default.hasSubs ) {
			[ switch( unit.get( \quality ), \good, \ls_, \better, \cs_, \ls_ ) ]
		} {
			[ switch( unit.get( \quality ), \good, \l_, \better, \c_, \l_ ) ]
		}
	].flop.collect(_.join);

	addDelay = calcDelayFocused.value( server ) - 0.06;
	if( unit.get( \minLatency ) == true ) {
		addDelayNormal = calcDelayNormal.value( server ) - 0.06;
	} {
		addDelayNormal = addDelay;
	};

	// make the panner synths, but don't send yet
	normalSynths = arrayConfs.collect({ |item|
		Synth.basicNew( defNames[0] ++ item.n, server );
	});
	focusSynths = arrayConfs.collect({ |item|
		Synth.basicNew( defNames[1] ++ item.n, server );
	});

	// make the pre-panner synth (has to know the nodeID's of the panner synths)
	prePanSynth = Synth( "wfsp_d_" ++ arrayConfs.size, arrayConfs.collect({ |item,i|
		[ "arrayConf" ++ i, item.asArray, "cornerPoints" ++ i, item.asCornersArray ]
	}).flatten( 1 ) ++ [
		\normalLevelBuses, normalLevelBuses,
		\focusLevelBuses, focusLevelBuses,
		\normalIDs, normalSynths.collect(_.nodeID),
		\focusIDs, focusSynths.collect(_.nodeID),
		\sendPointRate, WFSPositionTracker.getRate,
		\focusWidth, WFSSpeakerConf.default.focusWidth
	] ++ unit.getArgsFor( server, startPos ),
	arrayGroup, addAction: \addBefore
	);


	// send the panner synths
	n = 0;
	normalSynths.do({ |sn, i|
		server.sendMsg( *sn.newMsg( arrayGroup, [
			\gain, (WFSSpeakerConf.default.gain + arrayConfs[i].gain).dbamp,
			\arrayConf, arrayConfs[i],
			\amp, ("c" ++ normalLevelBuses[i]).asSymbol,
			\outOffset, n + arrayConfs[i].outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
			\arrayLimit, WFSSpeakerConf.default.arrayLimit,
			\arraySoftLimit, WFSSpeakerConf.default.arraySoftLimit,
			\focusWidth, WFSSpeakerConf.default.focusWidth,
			\addDelay, addDelayNormal,
		] ++ if( WFSSpeakerConf.default.hasSubs ) { [
			\subSpacing, arrayConfs[i].subSpacing,
			\subOffset, arrayConfs[i].subOffset,
			\subFreq, WFSSpeakerConf.default.subFreq,
			\subGain, WFSArrayPan.subGain,
		] } { [] } ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
		\addToTail )
		);
		n = n + arrayConfs[i].n;
	});

	n = 0;
	focusSynths.do({ |sn, i|
		server.sendMsg( *sn.newMsg( arrayGroup, [
			\gain, (WFSSpeakerConf.default.gain + arrayConfs[i].gain).dbamp,
			\arrayConf, arrayConfs[i],
			\amp, ("c" ++ focusLevelBuses[i]).asSymbol,
			\outOffset, n + arrayConfs[i].outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
			\arrayLimit, WFSSpeakerConf.default.arrayLimit,
			\arraySoftLimit, WFSSpeakerConf.default.arraySoftLimit,
			\focusWidth, WFSSpeakerConf.default.focusWidth,
			\addDelay, addDelay,
		] ++ if( WFSSpeakerConf.default.hasSubs ) { [
			\subSpacing, arrayConfs[i].subSpacing,
			\subOffset, arrayConfs[i].subOffset,
			\subFreq, WFSSpeakerConf.default.subFreq,
			\subGain, WFSArrayPan.subGain,
		] } { [] } ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
		\addToTail )
		);
		n = n + arrayConfs[i].n;
	});

	arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
	unit.addSynth( arrayGroup );

	prePanSynth;
};

dynamicPoint.env[ \wfsShouldPlayOnFunc ] = { |unit, target|
	WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
};

// preview panner; exact same parameters, different panner synthdefs
dynamicPoint.env[ \previewCreateSynthFunc ] = { |unit, target, startPos = 0|
	Synth( WFSPreviewSynthDefs.getDefName( getPreviewMode.( unit ), \d, \n ),
		unit.getArgsFor( target.asTarget.server, startPos ) ++
		[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) + unit.busOffset, \sendPointRate, WFSPositionTracker.getRate ],
		target, \addToTail
	);
};

staticPlane = HiddenFreeUdef( \staticPlane, [
	[ \point: 		0@0,  	WFSPlaneSpec( 200, 0.1 ) ],
	[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \nonsynth ],
	[ \latencyComp:  	0,		[0,1].asSpec, false, \init ],
	[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
	[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	[ \minLatency:  false, BoolSpec(false), false, \init ],
	[ \dynamic:		false,	BoolSpec(false), true, \nonsynth ], // force dynamic (hidden)
	[ \renderMode:			\previewMode, renderModeSpec, notPreviewMode, \nonsynth ],
	[ \busOffset:		0,	HardwareBusSpec(), notPreviewMode, \nonsynth ], // bus offset
] )
.addUEnv
.addUGlobalEQ
.addUIO( UIn, \ar, 0, 1 )
.inputIsEndPoint_( true )
.setSynthFunc_({ |unit ...keyValuePairs|
	keyValuePairs = (keyValuePairs ? []).clump(2) // exclude point for staticPlane
	.select({ |arr| arr[0] !== \point }).flatten(1);
	unit.synths.do({ |sn|
		var args = keyValuePairs;
		if( sn.class == Group ) { // is an array group or preview synth
			args = (args ? []).clump(2)
			.select({ |arr| arr[0] === \u_i_ar_0_bus }).flatten(1);
		};
		if( args.size > 0 ) {
			unit.subDef.prSetSynth( sn, *args );
		};
	})

});

staticPlane.env[ \wfsCreateSynthFunc ] = { |unit, target, startPos = 0|
	var server, n = 0;
	var arrayConfs;
	var arrayGroup;
	var prePanSynth;
	var planeLevels;
	var planeSynths = [];
	var defName;
	var crossfader;
	var gain;
	var point;
	var addDelay;

	server = target.asTarget.server;

	point = unit.get( \point ).value.asPoint;

	arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
	crossfader = WFSCrossfaderPlane( point, arrayConfs );

	arrayGroup = Group(target, addAction: \addToTail);

	defName = "wfsa_psn_";

	if( unit.get( \minLatency ) == true ) {
		addDelay = calcDelayNormal.value( server ) - 0.06;
	} {
		addDelay = calcDelayFocused.value( server ) - 0.06;
	};

	planeLevels = crossfader.crossfades;

	// make the pre-panner synth (has to know the nodeID's of the panner synths)
	prePanSynth = Synth( "wfsp_n_0",
		unit.getArgsFor( server, startPos ),
		arrayGroup, addAction: \addBefore
	);

	gain = WFSSpeakerConf.default.gain.dbamp * WFSSpeakerConf.default.planeWaveMakeUpGain;

	// make the panner synths
	n = 0;
	arrayConfs.do({ |ac, i|
		if( planeLevels[i] > 0 ) {
			planeSynths = planeSynths.add(							Synth( defName ++ ac.n, [
				\gain, gain * (ac.gain.dbamp),
				\arrayConf, ac,
				\outOffset, n + ac.outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
				\amp, planeLevels[i],
				\addDelay, addDelay,
			] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
			arrayGroup, addAction: \addToTail
			)
			);
		};
		n = n + arrayConfs[i].n;
	});

	arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
	unit.synths = unit.synths.add( arrayGroup );

	prePanSynth;
};

staticPlane.env[ \wfsShouldPlayOnFunc ] = { |unit, target|
	var arrays, crossfader;
	arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
	(arrays.size > 0) && {
		crossfader = WFSCrossfaderPlane( unit.get( \point ).uPrepareValue.asPoint, arrays );
		crossfader.crossfades.maxItem > 0;
	};
};

// preview panner; exact same parameters, different panner synthdefs
// the actual panning in this preview is no different to point sources
// since there is no difference in a stereo situation in this case
staticPlane.env[ \previewCreateSynthFunc ] = { |unit, target, startPos = 0|
	Synth( WFSPreviewSynthDefs.getDefName( getPreviewMode.( unit ), \s, \p ),
		unit.getArgsFor( target.asTarget.server, startPos ) ++
		[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) + unit.busOffset ],
		target, \addToTail
	);
};

dynamicPlane = HiddenFreeUdef( \dynamicPlane, [
	[ \point: 		0@0,  	WFSPlaneSpec( 200, 0.1 ) ],
	[ \type, 			\point, 	ListSpec( [ \point, \plane ] ), false, \nonsynth ],
	[ \quality:		0,		ListSpec([ \normal, \better ], 0 ), false, \nonsynth ], // for \l or \c
	[ \latencyComp:  	0,		[0,1].asSpec, false, \init ],
	[ \dbRollOff:		0,		[ -24, 0, \lin, 1, 0 ].asSpec ],
	[ \maxAmpRadius:	5,		[ 1, 5, \lin, 0, 5, "m" ].asSpec ],
	[ \minLatency:  false, BoolSpec(false), false, \init ],
	[ \dynamic:		false,	BoolSpec(false), true, \nonsynth ], // force dynamic (hidden)
	[ \renderMode:			\previewMode, renderModeSpec, notPreviewMode, \nonsynth ],
	[ \busOffset:		0,	HardwareBusSpec(), notPreviewMode, \nonsynth ], // bus offset
] )
.addUEnv
.addUGlobalEQ
.addUIO( UIn, \ar, 0, 1 )
.inputIsEndPoint_( true )
.setSynthFunc_({ |unit ...keyValuePairs|
	unit.synths.do({ |sn|
		var args = keyValuePairs;
		if( sn.class == Group ) { // is an array group or preview synth
			args = (args ? []).clump(2)
			.select({ |arr| [ \point, \u_i_ar_0_bus ].includes( arr[0] ) }).flatten(1);
		};
		if( args.size > 0 ) {
			unit.subDef.prSetSynth( sn, *args );
		};
	})

});
// dynamic plane wfs panner
dynamicPlane.env[ \wfsCreateSynthFunc ] = { |unit, target, startPos = 0|
	var server, n = 0;
	var arrayConfs;
	var arrayGroup;
	var prePanSynth;
	var planeSynths;
	var planeLevelBuses;
	var defName;
	var gain;
	var addDelay;

	server = target.asTarget.server;

	arrayConfs = WFSSpeakerConf.default.getArraysFor( server );

	planeLevelBuses = arrayConfs.collect({ |item, i| i + 2000 });

	arrayGroup = Group(target, addAction: \addToTail);

	defName =  "wfsa_pd" ++ switch( unit.get( \quality ), \good, \l_, \better, \c_, \l_ );

	if( unit.get( \minLatency ) == true ) {
		addDelay = calcDelayNormal.value( server ) - 0.06;
	} {
		addDelay = calcDelayFocused.value( server ) - 0.06;
	};

	// make the panner synths, but don't send yet
	planeSynths = arrayConfs.collect({ |item|
		Synth.basicNew( defName ++ item.n, server );
	});

	// make the pre-panner synth (has to know the nodeID's of the panner synths)
	prePanSynth = Synth( "wfsp_p_" ++ arrayConfs.size, arrayConfs.collect({ |item,i|
		[ "arrayConf" ++ i, item.asArray ]
	}).flatten( 1 ) ++ [
		\planeLevelBuses, planeLevelBuses,
		\planeIDs, planeSynths.collect(_.nodeID),
		\sendPointRate, WFSPositionTracker.getRate
	] ++ unit.getArgsFor( server, startPos ),
	arrayGroup, addAction: \addBefore
	);

	gain = WFSSpeakerConf.default.gain.dbamp * WFSSpeakerConf.default.planeWaveMakeUpGain;

	// send the panner synths
	n = 0;
	planeSynths.do({ |sn, i|
		server.sendMsg( *sn.newMsg( arrayGroup, [
			\gain, gain * (arrayConfs[i].gain.dbamp),
			\arrayConf, arrayConfs[i],
			\amp, ("c" ++planeLevelBuses[i]).asSymbol,
			\outOffset, n + arrayConfs[i].outputOffset + WFSSpeakerConf.getOutputBusStartOffset( server ),
			\addDelay, addDelay,
		] ++ U.formatArgs( [ \point, unit.point, \u_i_ar_0_bus, unit.u_i_ar_0_bus ], server, startPos ),
		\addToTail )
		);
		n = n + arrayConfs[i].n;
	});

	arrayGroup.freeAction_({ |group| unit.removeSynth( group ); });
	unit.addSynth( arrayGroup );

	prePanSynth;
};

dynamicPlane.env[ \wfsShouldPlayOnFunc ] = { |unit, target|
	WFSSpeakerConf.default.getArraysFor( target.asTarget.server ).size > 0;
};
// preview panner; exact same parameters, different panner synthdefs
// the actual panning in this preview is no different to point sources
// since there is no difference in a stereo situation in this case
dynamicPlane.env[ \previewCreateSynthFunc ] = { |unit, target, startPos = 0|
	Synth( WFSPreviewSynthDefs.getDefName( getPreviewMode.( unit ), \d, \p ),
		unit.getArgsFor(  target.asTarget.server, startPos ) ++
		[ \out, WFSSpeakerConf.getOutputBusStartOffset( target ) + unit.busOffset, \sendPointRate, WFSPositionTracker.getRate ],
		target, \addToTail
	);
};

[staticPoint, dynamicPoint, staticPlane, dynamicPlane ].do({ |def|
	def.createSynthFunc = { |unit, target, startPos = 0|
		if( WFSLib.previewMode === \off ) {
			def.env[ \wfsCreateSynthFunc ].value( unit, target, startPos );
		} {
			def.env[ \previewCreateSynthFunc ].value( unit, target, startPos );
		};
	};

	def.shouldPlayOnFunc = { |unit, target|
		if( WFSLib.previewMode === \off ) {
			def.env[ \wfsShouldPlayOnFunc ].value( unit, target );
		} {
			def.env[ \previewShouldPlayOnFunc ].value( unit, target );
		};
	};
});


multiDef = MultiUdef( \wfsSource, [staticPoint, dynamicPoint, staticPlane, dynamicPlane ], \wfs_panner, \mode, true )
.dontStoreArgNames_([ \mode ])
.chooseFunc_( { |args|
	var index, plane, dynamic;
	index = args.indexOf( \point );
	plane = args.indexOf( \type );
	dynamic = args.indexOf( \dynamic );
	if( plane.notNil ) { plane = args[ plane + 1 ] == \plane; } { plane = false };
	if( dynamic.notNil && { args[dynamic+1] == true } ) {
		dynamic = true;
	} {
		if( index.notNil && {
			args[index+1].isUMap && {
				args[index+1].allowedModes.includes( \init ).not;
			}
		}) {
			dynamic = true
		};
	};
	if( dynamic == true ) {
		if( plane, \dynamicPlane, \dynamicPoint );
	} {
		if( plane, \staticPlane, \staticPoint );
	};
} );
