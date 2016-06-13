// This test file (in conjuction with config in SwiftGeneratorTest) attempts to finds one include
// from a relative include path, one from an absolute include path, as well as on from the same
// root as the file itself.

include "from_include_path_1.inc"
include "from_include_path_2.inc"
include "from_same_path.inc"

// The test code checks that something got generated, so use a dummy struct for at least one
// file to show up
struct Dummy {

}
