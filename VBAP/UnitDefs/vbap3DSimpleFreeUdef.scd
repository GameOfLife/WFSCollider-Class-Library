FreeUdef( \vbap3D_Simple_Panner, [
    [ \anglesFromBus: 	false,	BoolSpec( false ) ],
    [ \angles: 	UnitSpherical(0,0), UnitSphericalSpec() ],
    [ \lag:		0,  	[ 0, 5, \lin, 0, 0 ].asSpec ], // lagtime for angles
    [ \spread:  1,	    [ 1, 100.0, \lin, 0, 0 ].asSpec ]
])
.createSynthFunc_({ |unit, target, startPos = 0|

    var conf = VBAPSpeakerConf.default;
    var buf = conf.buffers.at(target.asTarget.server);
    var n = conf.numSpeakers;
    var delays = conf.distances !? VBAPDistComp2.delayTimes(_);

    delays !? { |delays|
        Synth( ("VBAP_SIMPLE_3D_DistComp_"++n).asSymbol,
            unit.getArgsFor( target ) ++ [\u_bufnum,buf.bufnum, \u_delays, delays]
            , target, \addToTail );
    } ?? {
        Synth( ("VBAP_3D_SIMPLE_"++n).asSymbol,
            unit.getArgsFor( target ) ++ [\u_bufnum,buf.bufnum]
            , target, \addToTail );
    }
})
.category_(\spatialization)
.addUEnv
.addUGlobalEQ
.addUIO( UIn, \ar, 0, 1 )
.addUIO( UIn, \kr, 0, 2 );


