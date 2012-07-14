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
var def;

def = FreeUdef( \wfsStaticPoint, [
		[ \point: 		0@0,  	WFSPointSpec( 200, 0.1 ) ],
		[ \latencyComp:  	0,		[0,1].asSpec ],
		[ \dbRollOff:		-6,		[ -24, 0, \lin, 1, -6 ].asSpec ],
		[ \maxAmpRadius:	2,		[ 1, 5, \lin, 0, 2, "m" ].asSpec ],
	] )
	.category_( \wfs_panner )
	// first a few general things
	.envPut( \getArgsFor, { |evt, which, args, server, startPos = 0|
		if( which.notNil ) {
			U.formatArgs( 
				(args ? []).clump(2)
					.select({ |arr| which.includes( arr[0] ) }).flatten(1),
				server,
				startPos
			)
		} {
			[];
		}
	})
	.envPut( \getInitArgsFor, { |evt, defName, args, server, startPos = 0|
		var which;
		which = evt[ ("init_" ++ defName).asSymbol ] ++ evt[ ("set_" ++ defName).asSymbol ];
		evt.getArgsFor( which, args, server, startPos );
	})
	.envPut( \getSetArgsFor, { |evt, defName, args, server, startPos = 0|
		evt.getArgsFor( evt[ ("set_" ++ defName).asSymbol ], args, server, startPos );
	})
	.envPut( \init_wfsp, [ \latencyComp, \u_dur, \point ] ) // controls only set at init
	.envPut( \set_wfsp, [ // controls that can always be set
		\dbRollOff, \maxAmpRadius,
		\u_i_ar_0_bus, 
		\u_doneAction, \u_gain, \u_gate, \u_mute,
		\u_fadeIn, \u_fadeOut,
		\u_globalGain,
		\u_globalEQ_setting, \u_globalEQ_bypass, 
	] )
	.envPut( \init_wfsa, [ \u_i_ar_0_bus, \point ] )
	.addUEnv
	.addUGlobalEQ
	.addUIO( UIn, \ar, 0, 1 );

if( WFS.previewMode.isNil ) {	
	def.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var arrayConfs;
		var arrayGroup;
		var prePanSynth;
		var normalLevels, focusLevels;
		var normalSynths = [], focusSynths = [];
		var defNames;
		var crossfader;
		var focusMul;
		
		server = target.asTarget.server;
		
		arrayConfs = WFSSpeakerConf.default.getArraysFor( server );
		crossfader = WFSCrossfader( unit.get( \point ), arrayConfs );
		focusMul = WFSSpeakerConf.default.focusDetector.kr( unit.get( \point ) );
		
		arrayGroup = Group(target, addAction: \addToTail);
		
		defNames = [ "wfsa_nsn_", "wfsa_usn_" ]; // normal and uni static n
		
		normalLevels = crossfader.arraysShouldRun( false ) * crossfader.cornerfades;		focusLevels = crossfader.arraysShouldRun( true );
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			arrayGroup, addAction: \addBefore
		);
		
		// make the panner synths
		n = 0;
		arrayConfs.do({ |ac, i|
			if( normalLevels[i] > 0 ) {
				normalSynths = normalSynths.add(							Synth( defNames[0] ++ ac.n, [ 
							\arrayConf, ac, 
							\outOffset, n,
							\amp, normalLevels[i],
							\arrayLimit, WFSSpeakerConf.default.arrayLimit,
						] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
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
								\arrayConf, ac, 
								\outOffset, n,
								\amp, focusLevels[i],
								\arrayLimit, WFSSpeakerConf.default.arrayLimit,
							] ++ unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ), 
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
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.class, Group, \wfsa, Synth, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.def.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
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
	});
} {
	// preview panner; exact same parameters, different panner synthdefs
	def.createSynthFunc_({ |unit, target, startPos = 0|
		var server, n = 0;
		var prePanSynth;
		var previewSynth;
		
		server = target.asTarget.server;
		
		// make the pre-panner synth (has to know the nodeID's of the panner synths)
		prePanSynth = Synth( "wfsp_n_0",
		 	unit.def.env.getInitArgsFor( \wfsp, unit.args, server, startPos ),
			target, \addToTail
		);
		
		previewSynth = Synth( 
			WFSPreviewSynthDefs.getDefName( WFS.previewMode, \s ),
			unit.def.env.getInitArgsFor( \wfsa, unit.args, server, startPos ),
			target, \addToTail 
		);
				
		previewSynth.freeAction_({ |synth| unit.removeSynth( synth ); });
		unit.synths = unit.synths.add( previewSynth );
					
		prePanSynth;
	})
	.setSynthFunc_({ |unit ...keyValuePairs|
		unit.synths.do({ |sn|
			var server = sn.server;
			var key = switch( sn.defName.asString[..4].asSymbol, 
				\wfsx_, \wfsa, \wfsp_, \wfsp );
			server.sendSyncedBundle( Server.default.latency, nil, 
				*server.makeBundle( false, {
		    			sn.set( *unit.def.env.getSetArgsFor( key, keyValuePairs, server ) )
	    			})
	    		);
		})
	})
	.shouldPlayOnFunc_({ |unit, target|
		Server.default == target.asTarget.server; // safety measure: no output on wfs servers
	});		
};

def; // return def