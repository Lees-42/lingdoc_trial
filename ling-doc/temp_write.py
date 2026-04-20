content = open(r'D:\code_warehouse\Projects\LingDoc\temp_plan.md', 'r', encoding='utf-8').read()
with open(r'C:\Users\ASUS\.kimi\plans\doctor-mid-nite-miles-morales-sif.md', 'w', encoding='utf-8') as f:
    f.write(content)
print('Plan written successfully')
