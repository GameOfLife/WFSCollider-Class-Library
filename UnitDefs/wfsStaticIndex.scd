// speaker index on wfs system, static variant
// the difference with the dynamic version is that this one will only play on the server
// it needs to play on. The index can still be changed dynamically, but it won't sound on the
// other server if changed after start
(
var def;
def = FreeUdef( \wfsStaticIndex, [	
		[ \index: 		0,  		PositiveIntegerSpec() ],
		[ \gain:			-6,		[ -96, 0, \db, 1, -6].asSpec ],
] )
	.category_( \wfs_panner )
	.synthDef_(
		SynthDef( "wfsp_is", {
			var input, env, maxIndex;
			var gain = -10.dbamp;
			var index = 0, indexFromBus = 0;
		
			index = \index.kr(index); // not really static
			index = index - \startIndex.kr(0);
			gain = gain * \gain.kr(-6).dbamp;
			maxIndex = NumOutputBuses.ir; // only output to hardware outs
			
			gain = gain * InRange.kr(index, 0, maxIndex); // mute when out of range
			
			input = UIn.ar(0, 1) * gain * WFSLevelBus.kr * UEnv.kr;
			
			Out.ar( index.clip(0, maxIndex), input );
		})
	)
	.addSynthDefControls
	.shouldPlayOnFunc_({ |unit, target|
		target = target.asTarget.server;
		WFSSpeakerConf.includesServer( target ) && {
			unit.get( \index ).inclusivelyBetween( 
				WFSSpeakerConf.default.firstSpeakerIndexOf( target ),
				WFSSpeakerConf.default.lastSpeakerIndexOf( target )
			);
		};
	})
	.createSynthFunc_({ |unit, target|
		var startIndex;
		startIndex = WFSSpeakerConf.default.firstSpeakerIndexOf( target.asTarget.server );
		Synth( "wfsp_is", [ \startIndex, startIndex ] ++ unit.getArgsFor( target.asTarget.server ), 
			target, \addToTail );
	});

def.removeArgSpec( \startIndex ); // this is set by createSynthFunc

def
)