/*
\decode_direction

Decodes a single beam from an ambisonic soundfield signal.

angle: angle of the beam (-pi - pi)
elevation: elevation of the beam (-0.5pi, 0.5pi)
amp: amplitude (0-1)
mode: ambisonic mode (ambix - a.k.a. acn-sn3d, acn-n3d, fuma)

this is an Udef definition file
part of the WFSCollider-Class-Library default Udefs set
this Udef will only be loaded if the ATK library is installed
*/

if( 'Atk'.asClass.notNil ) {
	var defs, df;

	defs = [ 'ambix', 'acn-n3d', 'fuma' ].collect({ |mode|
		var subdefs;
		subdefs = (1.. switch( mode, 'fuma', 3, 5 ) ).collect({ |order|
			Udef( order, { |angle = 0.0, elevation = 0.0, amp = 1.0|
				var sig, numInputs, n, mtx;
				numInputs = (1,3..15).integrate[ order ];
				sig = UIn.ar( 0, numInputs, endPoint: true );

				if( mode != 'acn-n3d' ) {
					sig = HoaEncodeMatrix.ar( sig,
						HoaMatrixEncoder.newFormat( mode, order )
					);
				};

				sig = HoaDecodeDirection.ar( sig, angle, elevation,
					beamShape: \controlled,
					order: order
				);

				UOut.ar( 0, sig  * amp );

			},
			addToAll: false, extraPrefix: "decode_direction_%_".format( mode )
			).setSpec( \angle, AngleSpec() )
			.setSpec( \elevation, AngleSpec(-0.5pi, 0.5pi) )
		});

		MultiUdef( mode, subdefs, \ambisonics, \order, false, false );
	});

	MultiUdef( \decode_direction, defs, \ambisonics, \mode, false );
};