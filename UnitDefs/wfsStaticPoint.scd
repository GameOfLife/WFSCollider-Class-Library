// static point wfs panner
/*
This panner performs the crossfades on the language side, and only starts panner synths for 
those arrays that need them. The shouldPlayOn func also checks if there are any arrays
with non-zero level, and if not blocks playback for the given server. Normal sources
use the dedicated "wfsa_nsn_XX" arraypanners, in which the focused source per-speaker
crossfades are absent. Altogether this creates the ultimate most efficient panner.

The point can only be changed at init, and cannot be from a bus (which is why this is a
"static" point). The latencyComp parameter can also only be set at init. The dbRollOff 
and other global amplitude parameters however can be changed dynamically.

*/
(
FreeUdef( \wfsStaticPoint, [
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
		[ \arrayLimit:	1,		[ 0.164, 1, \lin, 0, 1 ].asSpec ],  // better name?
] )

	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server|
		if( which.notNil ) {
			(args ? []).clump(2)
				.select({ |arr| which.includes( arr[0] ) })
				.collect({ |arr| [arr[0],arr[1].asControlInputFor(server)] })
				.flatten;
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur ] ) // controls only set at init
	.envPut( \set_wfsp, [ // controls that can always be set
		\point, \dbRollOff, \maxAmpRadius,
		\u_i_ar_0_bus, 
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass, 
	] )
	.envPut( \init_wfsa, [ \arrayLimit, \arrayDbRollOff, \u_i_ar_0_bus, \point ] )
	.createSynthFunc_({ |unit, target|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var normalLevels, focusLevels;
		var normalSynths = [], focusSynths = [];
		var defNames;
		var crossfader;
		
		server = target.asTarget.server;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		crossfader = WFSCrossfader( unit.get( \point ), arrayConfs );
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defNames = [ "wfsa_nsn_", "wfsa_usn_" ]; // normal and uni static n
		
		normalLevels = crossfader.arraysShouldRun( false ) * crossfader.cornerfades;		focusLevels = crossfader.arraysShouldRun( true );
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.def.env.getInitArgsFor( \wfsp, unit.args, server ),
			arrayGroup, addAction: \addBefore
		);
		
		// make the panner synths
		n = 0;
		arrayConfs.do({ |ac, i|
			if( normalLevels[i] > 0 ) {
				normalSynths = normalSynths.add(							Synth( defNames[0] ++ ac.n, [ 
							\arrayConf, ac, 
							\outOffset, n,
							\amp, normalLevels[i]
						] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server ), 
						arrayGroup, addAction: \addToTail 
					) 
				);
			};
			n = n + arrayConfs[i].n;
		});
		
		n = 0;
		arrayConfs.do({ |ac, i|
			if( focusLevels[i] > 0 ) {
				focusSynths = focusSynths.add(							Synth( defNames[1] ++ ac.n, [ 
							\arrayConf, ac, 
							\outOffset, n,
							\amp, focusLevels[i]
						] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server ), 
						arrayGroup, addAction: \addToTail 
					) 
				);
			};
			n = n + arrayConfs[i].n;
		});
		
		arrayGroup.freeAction_({ |group| unit.synths.remove( group ); });
		unit.synths = unit.synths.add( arrayGroup );
		
		prePanSynth;
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			switch( sn.class,
				Group, {
					sn.set( *unit.def.env.getSetArgsFor( \wfsa, keyValuePairs, sn.server ) )
				},
				Synth, {
					sn.set( *unit.def.env.getSetArgsFor( \wfsp, keyValuePairs, sn.server ) )
				}
			)
		})
	})
	.shouldPlayOnFunc_({ |unit, target|
		var arrays, crossfader;
		arrays = WFSSpeakerConf.default.getArraysFor( target.asTarget.server );
		(arrays.size > 0) && {
			crossfader = WFSCrossfader( unit.get( \point ), arrays );
			[(crossfader.cornerfades * crossfader.arraysShouldRun( false )).maxItem,
			 crossfader.arraysShouldRun( true ).maxItem ].maxItem > 0
		};
	})
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 );
);