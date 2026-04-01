from main import convert_to_mpsz

test_cases = [
    (['1B', '9C', 'EW', '1F'], (['1s', '9m', '1z'], ['f1'])),
    (['2D', 'WD', '4S'], (['2p', '5z'], ['s4'])),
    (['UNKNOWN', '1B'], (['UNKNOWN', '1s'], [])),
]

for i, (input_data, expected) in enumerate(test_cases):
    result = convert_to_mpsz(input_data)
    print(f"Test Case {i+1}: Input={input_data}")
    print(f"  Result: {result}")
    print(f"  Expected: {expected}")
    assert result == expected
    print("  PASS")

print("All logic tests passed.")
