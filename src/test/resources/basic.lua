--- @module basic

--[[- A basic module
@type basic
]]
local basic = {}

--[[- Add two numbers together
@usage Do something simple.
 ```lua
print("Hello!")
print("World")
```

@source src/test/java/cc/tweaked/javadoc/files/BasicModule.java:21
@tparam number x The first number to add
@tparam number y The second number to add
@treturn number The added values
]]
function basic.add(x, y) end
