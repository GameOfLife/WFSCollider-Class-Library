// dynamic point wfs panner
(
var def;
def = FreeUdef( \wfsDynamicIndex, [	
		[ \indexFromBus: 	false,	BoolSpec( false ) ],
		[ \index: 		0,  		PositiveIntegerSpec() ],
		[ \gain:			-6,		[ -96, 0, \db, 1, -6].asSpec ],
] )
	.category_( \wfs_panner )
	.synthDef_(
		SynthDef( "wfsp_id", {
			var input, env, maxIndex;
			var gain = -10.dbamp;
			var index = 0, indexFromBus = 0;
			
			indexFromBus = \indexFromBus.kr(indexFromBus);
			index = (\index.kr(index) * (1-indexFromBus)) + (UIn.kr(0,1) * indexFromBus);
			
			index = index - \startIndex.kr(0);
			gain = gain * \gain.kr(-6).dbamp;
			maxIndex = NumOutputBuses.ir; // only output to hardware outs
			
			gain = gain * InRange.kr(index, 0, maxIndex); // mute when out of range
			
			input = UGlobalEQ.ar( UIn.ar(0, 1) ) * gain * UEnv.kr;
			
			Out.ar( index.clip(0, maxIndex), input );
		})
	)
	.addSynthDefControls
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.includesServer( target.asTarget.server );
	})
	.createSynthFunc_({ |unit, target|
		var startIndex;
		startIndex = WFSSpeakerConf.default.firstSpeakerIndexOf( target.asTarget.server );
		Synth( "wfsp_id", [ \startIndex, startIndex ] ++ unit.getArgsFor( target ), 
			target, \addToTail );
	});

def.removeArgSpec( \startIndex ); // this is set by createSynthFunc

def
)