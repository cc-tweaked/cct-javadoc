--- @module types

--[[- 
@source src/test/java/cc/tweaked/javadoc/files/InheritDoc.java:22
@type Adder
]]
local Adder = {}

--[[- Add two numbers together.


@source src/test/java/cc/tweaked/javadoc/files/InheritDoc.java:26
@tparam number x The first number to add.
@tparam number y The second number to add.
@treturn number The result of adding two numbers.
]]
function Adder.add(x, y) end
