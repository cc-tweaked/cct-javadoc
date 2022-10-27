--- @module types

--[[- [S, S]
@source src/test/java/cc/tweaked/javadoc/files/CustomType.java:25
@type Two
]]
local Two = {}

--[[- 
@source src/test/java/cc/tweaked/javadoc/files/CustomType.java:26
@treturn { types.One... } 
]]
function Two.getTwo() end
