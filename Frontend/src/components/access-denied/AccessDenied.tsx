import { EmptyState } from "../";

const AccessDenied = () => {
  return (
    <EmptyState
      imgSrc="/Assets/access_denied.svg"
      alt="Access Denied"
      title="Access Denied"
      message="Jira is not linked to your account. Please link Jira to access this page."
      fitParent
    />
  );
};

export default AccessDenied;
