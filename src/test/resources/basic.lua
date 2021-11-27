--- @module basic

--[[- A basic module
@source src/test/java/cc/tweaked/javadoc/files/BasicModule.java:10
@type basic
]]
local basic = {}

--[[- Add two numbers together.

This might be useful if you need to add two numbers and want to avoid
depending on jQuery.
@usage Do something simple.
```lua
print("Hello!")
print("World")
```

<code>&amp; &#42;</code>

@source src/test/java/cc/tweaked/javadoc/files/BasicModule.java:28
@tparam number x The first number to add
@tparam number y The second number to add
@treturn number The added values
]]
function basic.add(x, y) end
