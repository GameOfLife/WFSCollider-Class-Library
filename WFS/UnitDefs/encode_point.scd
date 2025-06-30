/*
\encode_point

Encodes a point to an ambisonic soundfield signal.

point: the point
elevation: elevation amount (-0.5pi - 0.5pi), 0.5pi is straight above, 0 is at horizon
dopplerAmt: amount of doppler effect
dbRollOff: amount of dB per distance doubling
maxAmpRadius: radius of area where no amplitude change is applied
mode: ambisonic mode (ambix - a.k.a. acn-sn3d, acn-n3d, fuma)

this is an Udef definition file
part of the WFSCollider-Class-Library default Udefs set
this Udef will only be loaded if the ATK library is installed
*/

if( 'Atk'.asClass.notNil ) {
	var defs, df;

	defs = [ 'ambix', 'acn-n3d', 'fuma' ].collect({ |mode|
		var subdefs;
		subdefs = (1.. switch( mode, 'fuma', 3, 7 ) ).collect({ |order|
			var def;
			def = Udef( order, { |point = #[0,0], elevation = 0, dopplerAmt = 1.0, dbRollOff = -6, maxAmpRadius = 5|
				var sig, numOutputs, n, mtx, dist;
				var limitAmp, amp;
				numOutputs = (1,3..15).integrate[ order ];
				sig = UIn.ar( 0, 1, endPoint: true );

				point = point.asPoint;

				dist = point.dist( 0@0 );

				limitAmp = maxAmpRadius.max(1).pow(dbRollOff/6); // limiting the limit to 1m to prevent large amp
				amp = (dist.pow(dbRollOff/6) / limitAmp).min( 1 );

				sig = DelayC.ar(sig * amp,  0.6, ( dist / WFSBasicPan.speedOfSound ) * dopplerAmt );
				sig = HoaEncodeDirection.ar( sig, point.angle - 0.5pi, elevation, order: order );

				if( mode != 'acn-n3d' ) {
					sig = HoaDecodeMatrix.ar( sig,
						HoaMatrixDecoder.newFormat( mode, order )
					);
				};

				UOut.ar( 0, sig );

			}, [
				[ \point: 0@0, WFSPointSpec( 200, 0.1 ) ],
				[ \elevation: 0, AngleSpec( -0.5pi, 0.5pi) ],
				[ \dopplerAmt: 1, [ 0, 1, \lin, 0, 1 ].asSpec ],
				[ \dbRollOff: -6, [ -24, 0, \lin, 1, -6 ].asSpec ],
				[ \maxAmpRadius: 5, [ 1, 5, \lin, 0, 5, "m" ].asSpec ],
			],
			addToAll: false, extraPrefix: "encode_point_%_".format( mode )
			);
		});

		MultiUdef( mode, subdefs, \ambisonics, \order, false, false );
	});

	MultiUdef( \encode_point, defs, \ambisonics, \mode, false );
};