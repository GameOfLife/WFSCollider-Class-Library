/*
\decode_binaural

Decodes ambisonic signals to binaural using hrtf files from the IEM plugin suite (https://plugins.iem.at/). Three formats (ambix (default), acn-n3d and fuma) are supported up to 7th order (fuma up to 3rd). AmbiX is also known as ACN-SN3D. ACN-N3D is the standard format used by the ATK library (https://www.ambisonictoolkit.net/). FuMa is short for Furse-Malham, an older format for ambisonics, often also known as B-format.

order: order of ambisonics signal (1-7, 1-3 in 'fuma' mode)
mode: ambisonics ordering and normalization ('ambix', 'acn-n3d', 'fuma')

--
this is an Udef definition file
part of the WFSCollider-Class-Library default Udefs set
*/


(
var defs, ambixMul, fumaOrder, fumaMul;

ambixMul =  [
	1.0, 1.7320508075688772, 2.2360679774997898, 2.6457513110645907,
	3.0, 3.3166247903553998, 3.6055512754639891, 3.8729833462074170
].collect({ |item,i| item!(i*2+1) }).flat;

fumaOrder = [ 0, 2, 3, 1, 8, 6, 4, 5, 7, 15, 13, 11, 9, 10, 12, 14 ];

fumaMul = [
	1.4142135623731, 1.7320508075689, 1.7320508075689, 1.7320508075689,
	1.9364916731037, 1.9364916731037, 2.2360679774998, 1.9364916731037,
	1.9364916731037, 2.0916500663352, 1.9720265943665, 2.2310934040909,
	2.6457513110646, 2.2310934040909, 1.9720265943665, 2.0916500663352
];

defs = [ 'ambix', 'acn-n3d', 'fuma' ].collect({ |mode|
	var subdefs;
	subdefs = (1.. switch( mode, 'fuma', 3, 7 ) ).collect({ |order|
		var orderKey;
		orderKey = "hrtfs%".format(order).asSymbol;
		Udef( order, {
			var startBuf, sig;
			startBuf = orderKey.ir([0,1,1,0])[0];
			sig = UIn.ar(0, (order+1).squared );
			switch( mode,
				'ambix', {
					sig = sig * ambixMul.keep( sig.size );
				},
				'fuma', {
					sig = sig[ fumaOrder.keep( sig.size ) ] * fumaMul.keep( sig.size );
				}
			);
			sig = UDecodeBinaural.ar( sig, startBuf );
			UOut.ar(0, sig );
		}, [ [ orderKey, UBinauralBufs( order ), AnythingSpec(), true ] ],
		addToAll: false,
		extraPrefix: "decode_binaural_%".format(mode),
		);
	});
	MultiUdef( mode, subdefs, 'utility', 'order', false, false );
});

MultiUdef( 'decode_binaural', defs, 'utility', 'mode', false, true )
.dontStoreArgNames_( (1..7).collect({ |x| "hrtfs%".format( x ).asSymbol }) )
)