const getPaddedItem = (
  targetClass: Record<string, string>,
  className: string,
  itemLabel: string,
  metaClassName: string
) => {
  return (
    <div className={`${targetClass[className]} ${targetClass[metaClassName]}`}>
      {itemLabel}
    </div>
  );
};

export default getPaddedItem;
