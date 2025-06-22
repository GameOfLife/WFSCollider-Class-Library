/*
\decode_panto

Decodes an ambisonic soundfield signal to a two-dimensional clockwise circle of points with equal distance to each other. When 'rotate' is 0 the first point will be at the front of the soundfield.

rotate: rotate the field clockwise (-pi - pi)
numChannels: number of points on the circle
order: ambisonic input order (1-7)
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
			MultiChannelUdef( order, { |rotate = 0.0|
				var sig, numInputs, n, mtx;
				numInputs = (1,3..15).integrate[ order ];
				sig = UIn.ar( 0, numInputs, endPoint: true );
				n = Udef.numChannels;
				if( mode != 'acn-n3d' ) {
					sig = HoaEncodeMatrix.ar( sig,
						HoaMatrixEncoder.newFormat( mode, order )
					);
				};
				sig = HoaRotate.ar( sig, rotate.neg, order );
				if( ((n - 1) / 2).asInteger < order ) {
					mtx = HoaMatrixDecoder.newPanto( n*2,
						orientation: \point,
						beamShape: \controlled,
						match: \amp,
						order: order
					);
					mtx.slotPut( \directions, mtx.directions.clump(2).flop[0] );
					mtx.slotPut( \matrix, (mtx.matrix.asArray.clump(2).flop[0] * 2).as( Matrix ) );
				} {
					mtx = HoaMatrixDecoder.newPanto( n,
						orientation: \point,
						beamShape: \controlled,
						match: \amp,
						order: order
					);
				};

				sig = HoaDecodeMatrix.ar( sig, mtx )[(1..n-1)++0].reverse;

				UOut.ar( 0, sig );

			}, channels: [4,5,6,7,8,10,12,16,24,32],
			addToAll: false, extraPrefix: "decode_panto_%_".format( mode )
			).setSpec( \rotate, AngleSpec() );
		});

		MultiUdef( mode, subdefs, \ambisonics, \order, false, false );
	});

	MultiUdef( \decode_panto, defs, \ambisonics, \mode, false );
};