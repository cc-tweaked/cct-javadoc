--- @module basic

--[[- A basic module
@source src/test/java/cc/tweaked/javadoc/files/BasicModule.java:10
@type basic
]]
local basic = {}

--[[- Add two numbers together.

This might be useful if you need to add two numbers and want to avoid
depending on jQuery.

One more paragraph.


 - We just want to check that we desugar lists into other lists. This ensures that one can correctly use
   markdown features (otherwise they're nested within HTML, which stinks).
 - And

   another entry.


<customTag attribute="value"></customTag>
<custom-tag attribute="value"></custom-tag>
@usage Do something simple.
```lua
print("Hello!")
print("World")
```
@usage Another example
```lua {attribute=value}
print("Test")
```

<code>&amp; &#42;</code>

@source src/test/java/cc/tweaked/javadoc/files/BasicModule.java:45
@tparam number x The first number to add
@tparam number y The second number to add
@treturn number The added values
]]
function basic.add(x, y) end
