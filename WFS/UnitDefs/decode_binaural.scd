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
var category = 'utility';

if( 'Atk'.asClass.notNil ) { category = 'ambisonics' };

ambixMul = (1,3..15).collect({ |x| x.sqrt!x }).flat;

fumaOrder = [ 0, 2, 3, 1, 8, 6, 4, 5, 7, 15, 13, 11, 9, 10, 12, 14 ];

fumaMul = [
	2, 3, 3, 3, 3.75, 3.75, 5, 3.75,
	3.75, 4.375, 3 + (8/9), 4 + (44/45),
	7, 4 + (44/45), 3 + (8/9), 4.375
].sqrt;

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
	MultiUdef( mode, subdefs, category, 'order', false, false );
});

MultiUdef( 'decode_binaural', defs, category, 'mode', false, true )
.dontStoreArgNames_( (1..7).collect({ |x| "hrtfs%".format( x ).asSymbol }) )
)