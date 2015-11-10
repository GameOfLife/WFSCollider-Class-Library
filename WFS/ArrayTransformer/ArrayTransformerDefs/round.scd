ArrayTransformerDef( \round, { |f, obj|
	obj.round( f.get( \round ) );
}, [ \round, 0.0] );