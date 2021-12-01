UNodeIDAllocator {
	// a node allocator excluding any nodeID above 2**24
	// because those are incompatible with 32bits floats
	// and therefore cannot be used for pausing/unpausing
	// nodes from within synths.

	classvar <>maxUsers = 2;
	var <user, initTemp, temp, perm, mask, permFreed;
	var <wrapper;

	*new { arg user=0, initTemp = 1000;
		if (user >= maxUsers) { "NodeIDAllocator maxUsers (%) exceeded".format( maxUsers ).error; ^nil };
		^super.newCopyArgs(user, initTemp).reset
	}

	idOffset { ^(wrapper + 1) * user }

	powerOfTwo { ^25 - maxUsers }
	getNumIDs { ^(2 ** this.powerOfTwo).asInteger }

	numIDs { ^wrapper + 1 }

	reset {
		wrapper = this.getNumIDs - 1;
		mask = user << this.powerOfTwo;
		temp = initTemp;
		perm = 2;
		permFreed = IdentitySet.new;
	}
	alloc {
		var x;
		x = temp;
		temp = (x + 1).wrap(initTemp, wrapper);
		//"allocated: %\n".postf( x | mask );
		^x | mask
	}
	allocPerm {
		var x;
		if(permFreed.size > 0) {
			x = permFreed.minItem;
			permFreed.remove(x);
		} {
			x = perm;
			perm = (x + 1).min(initTemp - 1);
		}
		^x | mask
	}
	freePerm { |id|
			// should not add a temp node id to the freed-permanent collection
		id = id bitAnd: wrapper;
		if(id < initTemp) { permFreed.add(id) }
	}
}
